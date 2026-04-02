/* ══════════════════════════════════════════════════════════
   Mini Marketplace — main.js
   Handles: Toast, Cart, Order placement, Product CRUD,
            Navbar, Search debounce, Quantity controls
   ══════════════════════════════════════════════════════════ */

'use strict';

/* ── Toast System ─────────────────────────────────────────── */
const Toast = (() => {
  let container = null;

  function getContainer() {
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      document.body.appendChild(container);
    }
    return container;
  }

  function show(message, type = 'success', duration = 3500) {
    const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
    const toast = document.createElement('div');
    toast.className = `toast toast--${type}`;
    toast.innerHTML = `
      <span class="toast__icon">${icon}</span>
      <span>${message}</span>
      <button class="toast__close" aria-label="Close">×</button>`;

    getContainer().appendChild(toast);
    requestAnimationFrame(() => { requestAnimationFrame(() => toast.classList.add('show')); });

    const dismiss = () => {
      toast.classList.remove('show');
      setTimeout(() => toast.remove(), 400);
    };
    toast.querySelector('.toast__close').addEventListener('click', dismiss);
    setTimeout(dismiss, duration);
  }

  return { success: (m) => show(m, 'success'), error: (m) => show(m, 'error'), info: (m) => show(m, 'info') };
})();

/* ── API Helper ───────────────────────────────────────────── */
const API = (() => {
  const CSRF_TOKEN = document.querySelector('meta[name="_csrf"]')?.content;
  const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;

  async function request(url, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    if (CSRF_TOKEN && CSRF_HEADER) headers[CSRF_HEADER] = CSRF_TOKEN;

    const response = await fetch(url, { ...options, headers });
    if (!response.ok) {
      let errMsg = `HTTP ${response.status}`;
      try {
        const err = await response.json();
        errMsg = err.message || errMsg;
      } catch (_) {}
      throw new Error(errMsg);
    }
    if (response.status === 204) return null;
    return response.json();
  }

  return {
    get:    (url)          => request(url, { method: 'GET' }),
    post:   (url, body)    => request(url, { method: 'POST',   body: JSON.stringify(body) }),
    put:    (url, body)    => request(url, { method: 'PUT',    body: JSON.stringify(body) }),
    delete: (url)          => request(url, { method: 'DELETE' }),
  };
})();

/* ── Cart (sessionStorage) ────────────────────────────────── */
const Cart = (() => {
  const KEY = 'mm_cart';

  function load()        { try { return JSON.parse(sessionStorage.getItem(KEY)) || []; } catch { return []; } }
  function save(items)   { sessionStorage.setItem(KEY, JSON.stringify(items)); updateBadge(); }
  function getAll()      { return load(); }
  function count()       { return load().reduce((s, i) => s + i.quantity, 0); }
  function clear()       { sessionStorage.removeItem(KEY); updateBadge(); }

  function addItem(productId, name, price, quantity = 1) {
    const items = load();
    const existing = items.find(i => i.productId === productId);
    if (existing) {
      existing.quantity += quantity;
    } else {
      items.push({ productId, name, price, quantity });
    }
    save(items);
    Toast.success(`"${name}" added to cart`);
    return items;
  }

  function removeItem(productId) {
    const items = load().filter(i => i.productId !== productId);
    save(items);
  }

  function updateQty(productId, qty) {
    const items = load();
    const item = items.find(i => i.productId === productId);
    if (item) { item.quantity = Math.max(1, qty); save(items); }
  }

  function total() {
    return load().reduce((sum, i) => sum + (parseFloat(i.price) * i.quantity), 0).toFixed(2);
  }

  function updateBadge() {
    const badge = document.getElementById('cart-badge');
    if (badge) {
      const n = count();
      badge.textContent = n;
      badge.style.display = n > 0 ? 'flex' : 'none';
    }
  }

  return { getAll, count, total, addItem, removeItem, updateQty, clear, updateBadge, load };
})();

/* ── Product Detail Page ──────────────────────────────────── */
function initProductDetail() {
  const addToCartBtn = document.getElementById('add-to-cart-btn');
  if (!addToCartBtn) return;

  const productId   = parseInt(addToCartBtn.dataset.productId);
  const productName = addToCartBtn.dataset.productName;
  const productPrice= addToCartBtn.dataset.productPrice;
  const qtyInput    = document.getElementById('qty-input');

  addToCartBtn.addEventListener('click', () => {
    const qty = parseInt(qtyInput?.value || 1);
    if (isNaN(qty) || qty < 1) { Toast.error('Please enter a valid quantity.'); return; }
    Cart.addItem(productId, productName, productPrice, qty);
  });

  // Quantity +/- buttons
  document.getElementById('qty-plus')?.addEventListener('click',  () => { if (qtyInput) qtyInput.value = Math.min(99, parseInt(qtyInput.value) + 1); });
  document.getElementById('qty-minus')?.addEventListener('click', () => { if (qtyInput) qtyInput.value = Math.max(1,  parseInt(qtyInput.value) - 1); });
}

/* ── Product List Page ────────────────────────────────────── */
function initProductList() {
  document.querySelectorAll('.add-to-cart-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const productId = parseInt(btn.dataset.productId, 10);
      const productName = btn.dataset.productName;
      const productPrice = btn.dataset.productPrice;

      if (!Number.isFinite(productId) || !productName || !productPrice) {
        Toast.error('Unable to add this product right now.');
        return;
      }

      Cart.addItem(productId, productName, productPrice, 1);
    });
  });
}

/* ── Cart Page / Modal ────────────────────────────────────── */
function renderCartItems() {
  const container = document.getElementById('cart-items-container');
  const totalEl   = document.getElementById('cart-total');
  if (!container) return;

  const items = Cart.getAll();
  if (items.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-state__icon">🛒</div>
        <h3>Your cart is empty</h3>
        <p>Browse products and add items to get started.</p>
        <a href="/products" class="btn btn--green" style="margin-top:1rem">Shop Now</a>
      </div>`;
    if (totalEl) totalEl.textContent = '$0.00';
    return;
  }

  container.innerHTML = items.map(item => `
    <div class="order-item-row" data-product-id="${item.productId}">
      <div>
        <strong>${item.name}</strong>
        <div style="font-size:.8rem;color:var(--text-light)">$${parseFloat(item.price).toFixed(2)} × ${item.quantity}</div>
      </div>
      <div style="display:flex;align-items:center;gap:.5rem">
        <strong style="color:var(--green-700)">$${(parseFloat(item.price) * item.quantity).toFixed(2)}</strong>
        <button class="btn btn--danger btn--sm remove-item-btn" data-id="${item.productId}">✕</button>
      </div>
    </div>`).join('');

  if (totalEl) totalEl.textContent = '$' + Cart.total();

  container.querySelectorAll('.remove-item-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      Cart.removeItem(parseInt(btn.dataset.id));
      renderCartItems();
    });
  });
}

/* ── Place Order ──────────────────────────────────────────── */
async function placeOrder(buyerId) {
  const items = Cart.getAll();
  if (items.length === 0) { Toast.error('Your cart is empty.'); return; }

  const btn = document.getElementById('place-order-btn');
  if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner"></span> Placing Order…'; }

  try {
    const payload = {
      buyerId,
      items: items.map(i => ({ productId: i.productId, quantity: i.quantity }))
    };
    const result = await API.post('/api/orders', payload);
    Cart.clear();
    Toast.success(`Order #${result.orderId} placed! Total: $${result.totalPrice}`);
    setTimeout(() => window.location.href = '/order-history', 1800);
  } catch (err) {
    Toast.error('Order failed: ' + err.message);
    if (btn) { btn.disabled = false; btn.textContent = 'Place Order'; }
  }
}

/* ── Product Form (Add/Edit) ──────────────────────────────── */
async function submitProductForm(event) {
  event.preventDefault();
  const form    = event.target;
  const sellerId= form.dataset.sellerId;
  const productId = form.dataset.productId;
  const isEdit  = !!productId;

  const payload = {
    name:        form.querySelector('[name="name"]').value.trim(),
    description: form.querySelector('[name="description"]').value.trim(),
    price:       parseFloat(form.querySelector('[name="price"]').value),
    quantity:    parseInt(form.querySelector('[name="quantity"]').value),
  };

  const btn = form.querySelector('[type="submit"]');
  if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner"></span> Saving…'; }

  try {
    if (isEdit) {
      await API.put(`/api/products/${productId}?sellerId=${sellerId}`, payload);
      Toast.success('Product updated!');
    } else {
      await API.post(`/api/products?sellerId=${sellerId}`, payload);
      Toast.success('Product listed!');
      form.reset();
    }
    setTimeout(() => window.location.href = '/products', 1500);
  } catch (err) {
    Toast.error('Failed: ' + err.message);
    if (btn) { btn.disabled = false; btn.textContent = isEdit ? 'Update Product' : 'List Product'; }
  }
}

/* ── Delete Product ───────────────────────────────────────── */
async function deleteProduct(productId, sellerId) {
  if (!confirm('Delete this product? This cannot be undone.')) return;
  try {
    await API.delete(`/api/products/${productId}?sellerId=${sellerId}`);
    Toast.success('Product deleted.');
    const card = document.querySelector(`[data-product-id="${productId}"]`);
    if (card) card.remove();
  } catch (err) {
    Toast.error('Delete failed: ' + err.message);
  }
}

/* ── Search Debounce ──────────────────────────────────────── */
function initSearchDebounce() {
  const form  = document.getElementById('search-form');
  const input = document.getElementById('search-input');
  if (!input || !form) return;

  let timer;
  input.addEventListener('input', () => {
    clearTimeout(timer);
    timer = setTimeout(() => {
      if (input.value.trim().length >= 2 || input.value.trim().length === 0) {
        form.submit();
      }
    }, 450);
  });
}

/* ── Navbar Mobile Toggle ─────────────────────────────────── */
function initNavbar() {
  const toggle = document.getElementById('nav-toggle');
  const links  = document.getElementById('nav-links');
  if (!toggle || !links) return;

  toggle.addEventListener('click', () => {
    links.classList.toggle('open');
    toggle.setAttribute('aria-expanded', links.classList.contains('open'));
  });

  // Close on outside click
  document.addEventListener('click', e => {
    if (!toggle.contains(e.target) && !links.contains(e.target)) {
      links.classList.remove('open');
    }
  });

  // Active link highlight
  const currentPath = window.location.pathname;
  links.querySelectorAll('a').forEach(a => {
    if (a.getAttribute('href') === currentPath ||
        (a.getAttribute('href') !== '/' && currentPath.startsWith(a.getAttribute('href')))) {
      a.classList.add('active');
    }
  });
}

/* ── Order History Accordion ──────────────────────────────── */
function initOrderAccordion() {
  document.querySelectorAll('.order-card__header').forEach(header => {
    header.addEventListener('click', () => {
      const items = header.nextElementSibling;
      if (!items) return;
      const isOpen = items.style.display !== 'none' && items.style.display !== '';
      items.style.display = isOpen ? 'none' : 'block';
      const arrow = header.querySelector('.order-arrow');
      if (arrow) arrow.textContent = isOpen ? '▼' : '▲';
    });
  });
}

/* ── Init ─────────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
  Cart.updateBadge();
  initNavbar();
  initProductDetail();
  initProductList();
  initSearchDebounce();
  initOrderAccordion();
  renderCartItems();

  // Attach product form handler
  const productForm = document.getElementById('product-form');
  if (productForm) productForm.addEventListener('submit', submitProductForm);

  // Attach place-order button
  const placeOrderBtn = document.getElementById('place-order-btn');
  if (placeOrderBtn) {
    placeOrderBtn.addEventListener('click', () => {
      const buyerId = parseInt(placeOrderBtn.dataset.buyerId);
      placeOrder(buyerId);
    });
  }

  // Dismiss alerts
  document.querySelectorAll('.alert[data-dismiss]').forEach(alert => {
    setTimeout(() => alert.remove(), 4000);
  });
});

// Expose for inline usage
window.Toast     = Toast;
window.Cart      = Cart;
window.API       = API;
window.deleteProduct = deleteProduct;
