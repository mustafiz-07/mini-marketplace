package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.UserRequestDTO;
import com.example.minimarketplace.model.Role;
import com.example.minimarketplace.model.User;
import com.example.minimarketplace.repository.UserRepository;
import com.example.minimarketplace.service.OrderService;
import com.example.minimarketplace.service.ProductService;
import com.example.minimarketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final ProductService productService;
    private final OrderService   orderService;
    private final UserService    userService;
    private final UserRepository userRepository;

    // ── Home ───────────────────────────────────────────────────────────────────
    @GetMapping("/")
    public String home(Model model, Authentication auth) {
        model.addAttribute("featuredProducts",
                productService.findAvailable().stream().limit(6).toList());
        injectBuyerId(model, auth);
        return "home";
    }

    // ── Product List ───────────────────────────────────────────────────────────
    @GetMapping("/products")
    public String productList(
            @RequestParam(required = false) String search,
            Model model, Authentication auth) {
        model.addAttribute("products",
                search != null && !search.isBlank()
                        ? productService.search(search)
                        : productService.findAvailable());
        model.addAttribute("search", search);
        injectBuyerId(model, auth);
        return "product-list";
    }

    // ── Product Detail ─────────────────────────────────────────────────────────
    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id, Model model, Authentication auth) {
        model.addAttribute("product", productService.findById(id));
        injectBuyerId(model, auth);
        return "product-details";
    }

    // ── Add Product (Seller only) ──────────────────────────────────────────────
    @GetMapping("/add-product")
    public String addProductForm(Model model, Authentication auth) {
        model.addAttribute("currentUser", resolveUser(auth));
        injectBuyerId(model, auth);
        return "add-product";
    }

    // ── Login ──────────────────────────────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {
        if (error != null) model.addAttribute("loginError", "Invalid email or password.");
        if (logout != null) model.addAttribute("logoutMsg", "You have been logged out.");
        return "login";
    }

    // ── Register ───────────────────────────────────────────────────────────────
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("userRequest", new UserRequestDTO("", "", "", Role.BUYER));
        model.addAttribute("roles", Role.values());
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(
            @Valid @ModelAttribute("userRequest") UserRequestDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "register";
        }
        try {
            userService.register(dto);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Account created! Please login.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("roles", Role.values());
            return "register";
        }
    }

    // ── Order History ──────────────────────────────────────────────────────────
    @GetMapping("/order-history")
    public String orderHistory(Model model, Authentication auth) {
        model.addAttribute("orders", List.of());
        model.addAttribute("totalSpent", BigDecimal.ZERO);
        model.addAttribute("itemsPurchased", 0);

        User user = resolveUser(auth);
        if (user != null && user.getRole() == Role.SELLER) {
            return "redirect:/seller/orders";
        }
        if (user != null) {
            List<com.example.minimarketplace.model.Order> orders = orderService.findByBuyer(user.getId());
            BigDecimal totalSpent = orders.stream()
                    .map(com.example.minimarketplace.model.Order::getTotalPrice)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int itemsPurchased = orders.stream()
                    .mapToInt(o -> o.getItems() != null ? o.getItems().size() : 0)
                    .sum();

            model.addAttribute("orders", orders);
            model.addAttribute("totalSpent", totalSpent);
            model.addAttribute("itemsPurchased", itemsPurchased);
            model.addAttribute("currentUser", user);
        }
        injectBuyerId(model, auth);
        return "order-history";
    }

    // ── Seller Orders Dashboard ───────────────────────────────────────────────
    @GetMapping({"/seller/orders", "/seller/order"})
    public String sellerOrders(Model model, Authentication auth) {
        User seller = resolveUser(auth);
        if (seller == null) {
            return "redirect:/login";
        }

        List<OrderService.SellerOrderView> sellerOrders = orderService.findForSeller(seller.getId());
        BigDecimal totalRevenue = sellerOrders.stream()
                .map(OrderService.SellerOrderView::sellerTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItemsSold = sellerOrders.stream()
                .flatMap(order -> order.items().stream())
                .mapToInt(OrderService.SellerOrderLineItem::quantity)
                .sum();

        model.addAttribute("sellerOrders", sellerOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalItemsSold", totalItemsSold);
        model.addAttribute("currentUser", seller);
        injectBuyerId(model, auth);
        return "seller-orders";
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────
    private User resolveUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    private void injectBuyerId(Model model, Authentication auth) {
        User user = resolveUser(auth);
        model.addAttribute("buyerId", user != null ? user.getId() : 0L);
    }
}
