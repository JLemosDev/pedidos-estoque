"use strict";
const $ = (selector) => document.querySelector(selector);
const state = {
  authorization: "",
  section: "produtos",
  produtos: [],
  pedidos: [],
  movimentos: [],
  busy: false,
};
const money = (value) =>
  Number(value).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
const date = (value) =>
  new Date(value).toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  });
const escape = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (char) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        char
      ],
  );
const statuses = {
  RASCUNHO: ["Rascunho", "neutral"],
  CONFIRMADO: ["Confirmado", ""],
  CANCELADO: ["Cancelado", "danger"],
};
const motives = {
  SALDO_INICIAL: "Saldo inicial",
  REPOSICAO: "Reposição",
  PEDIDO_CONFIRMADO: "Pedido confirmado",
  PEDIDO_CANCELADO: "Pedido cancelado",
};
const sections = {
  produtos: [
    "Produtos",
    "Seu catálogo e os saldos disponíveis, em um só lugar.",
    "Catálogo de produtos",
    "+ Novo produto",
  ],
  pedidos: [
    "Pedidos",
    "Crie, confirme e acompanhe cada pedido da loja.",
    "Pedidos da loja",
    "+ Novo pedido",
  ],
  movimentos: [
    "Movimentações",
    "O histórico de entradas e saídas do seu estoque.",
    "Últimas 100 movimentações",
    "",
  ],
};

function notice(message, error = false) {
  $("#notice").textContent = message;
  $("#notice").className = error ? "error" : "";
}

async function api(path, options = {}) {
  const response = await fetch("/api/" + path, {
    ...options,
    headers: {
      Authorization: state.authorization,
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...options.headers,
    },
    cache: "no-store",
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    if (response.status === 401 && state.authorization) logout();
    throw new Error(
      data.error ||
        data.message ||
        data.mensagem ||
        "Não foi possível concluir. Confira a conexão e tente novamente.",
    );
  }
  return data;
}

async function refresh() {
  $("#content").setAttribute("aria-busy", "true");
  try {
    const [produtos, pedidos, movimentos] = await Promise.all([
      api("produtos"),
      api("pedidos"),
      api("produtos/movimentacoes"),
    ]);
    Object.assign(state, { produtos, pedidos, movimentos });
    render();
  } finally {
    $("#content").setAttribute("aria-busy", "false");
  }
}

function logout() {
  state.authorization = "";
  state.produtos = [];
  state.pedidos = [];
  state.movimentos = [];
  $("#application").hidden = true;
  $("#login-screen").hidden = false;
  $("#login-form").elements.senha.value = "";
  $("#editor").close();
}

$("#login-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const button = form.querySelector("button");
  const fields = new FormData(form);
  const raw = new TextEncoder().encode(
    fields.get("usuario") + ":" + fields.get("senha"),
  );
  state.authorization =
    "Basic " +
    btoa(Array.from(raw, (byte) => String.fromCharCode(byte)).join(""));
  button.disabled = true;
  $("#login-error").textContent = "";
  try {
    await refresh();
    $("#login-screen").hidden = true;
    $("#application").hidden = false;
    form.elements.senha.value = "";
  } catch (error) {
    state.authorization = "";
    $("#login-error").textContent = error.message;
  } finally {
    button.disabled = false;
  }
});
$("#logout").addEventListener("click", logout);
document.querySelectorAll("[data-section]").forEach((button) =>
  button.addEventListener("click", () => {
    state.section = button.dataset.section;
    $("#search").value = "";
    notice("");
    render();
  }),
);
$("#search").addEventListener("input", renderContent);
$("#main-action").addEventListener("click", () =>
  state.section === "produtos" ? productEditor() : orderEditor(),
);
$("#close-dialog").addEventListener("click", () => {
  if (!state.busy) $("#editor").close();
});

function render() {
  const [title, description, listTitle, action] = sections[state.section];
  $("#section-title").textContent = title;
  $("#section-description").textContent = description;
  $("#list-title").textContent = listTitle;
  $("#main-action").textContent = action;
  $("#main-action").hidden = !action;
  document
    .querySelectorAll("[data-section]")
    .forEach((button) =>
      button.classList.toggle(
        "active",
        button.dataset.section === state.section,
      ),
    );
  const confirmed = state.pedidos.filter((p) => p.status === "CONFIRMADO");
  const metrics = [
    [
      "Produtos no catálogo",
      state.produtos.length,
      "Códigos únicos por produto",
    ],
    [
      "Unidades disponíveis",
      state.produtos.reduce((sum, p) => sum + p.estoque, 0),
      "Saldo atual registrado",
    ],
    [
      "Estoque baixo",
      state.produtos.filter((p) => p.estoque <= 5).length,
      "Produtos com até 5 unidades",
    ],
    [
      "Pedidos confirmados",
      confirmed.length,
      money(confirmed.reduce((sum, p) => sum + Number(p.total), 0)) +
        " em pedidos",
    ],
  ];
  $("#metrics").innerHTML = metrics
    .map(
      ([label, value, hint]) =>
        '<div class="metric"><span class="label">' +
        escape(label) +
        "</span><strong>" +
        escape(value) +
        "</strong><small>" +
        escape(hint) +
        "</small></div>",
    )
    .join("");
  renderContent();
}

function renderContent() {
  const search = $("#search").value.toLocaleLowerCase("pt-BR");
  const matches = (text) =>
    String(text).toLocaleLowerCase("pt-BR").includes(search);
  let headings = [],
    rows = [];
  if (state.section === "produtos") {
    headings = [
      "PRODUTO",
      "PREÇO UNITÁRIO",
      "SALDO",
      "DISPONIBILIDADE",
      "AÇÕES",
    ];
    rows = state.produtos
      .filter((p) => matches(p.nome + " " + p.sku))
      .map(
        (p) =>
          "<tr><td><strong>" +
          escape(p.nome) +
          "</strong><small>" +
          escape(p.sku) +
          "</small></td><td>" +
          money(p.preco) +
          "</td><td><strong>" +
          p.estoque +
          '</strong> un.</td><td><span class="pill ' +
          (p.estoque <= 5 ? "warning" : "") +
          '">' +
          (p.estoque === 0
            ? "Sem estoque"
            : p.estoque <= 5
              ? "Estoque baixo"
              : "Disponível") +
          '</span></td><td><div class="actions"><button data-action="edit" data-id="' +
          p.id +
          '">Editar</button><button data-action="replenish" data-id="' +
          p.id +
          '">+ Repor</button></div></td></tr>',
      );
  } else if (state.section === "pedidos") {
    headings = ["PEDIDO / CLIENTE", "ITENS", "TOTAL", "STATUS", "AÇÕES"];
    rows = state.pedidos
      .filter((p) => matches(p.cliente + " " + p.id))
      .map((p) => {
        const [label, style] = statuses[p.status];
        const actions =
          (p.status === "RASCUNHO"
            ? '<button data-action="confirm" data-id="' +
              p.id +
              '">Confirmar</button>'
            : "") +
          (p.status !== "CANCELADO"
            ? '<button data-action="cancel" data-id="' +
              p.id +
              '">Cancelar</button>'
            : "");
        return (
          "<tr><td><strong>#" +
          p.id +
          " · " +
          escape(p.cliente) +
          "</strong><small>" +
          date(p.criadoEm) +
          "</small></td><td>" +
          p.itens
            .map((i) => escape(i.produtoNome) + " × " + i.quantidade)
            .join("<br>") +
          "</td><td>" +
          money(p.total) +
          '</td><td><span class="pill ' +
          style +
          '">' +
          label +
          '</span></td><td><div class="actions">' +
          actions +
          "</div></td></tr>"
        );
      });
  } else {
    headings = ["PRODUTO", "MOVIMENTO", "QUANTIDADE", "PEDIDO", "DATA"];
    rows = state.movimentos
      .filter((m) =>
        matches(
          (state.produtos.find((p) => p.id === m.produtoId)?.nome || "") +
            " " +
            motives[m.motivo],
        ),
      )
      .map(
        (m) =>
          "<tr><td>" +
          escape(
            state.produtos.find((p) => p.id === m.produtoId)?.nome ||
              "Produto #" + m.produtoId,
          ) +
          "</td><td>" +
          escape(motives[m.motivo] || m.motivo) +
          '</td><td><strong class="' +
          (m.quantidade > 0 ? "positive" : "negative") +
          '">' +
          (m.quantidade > 0 ? "+" : "") +
          m.quantidade +
          "</strong> un.</td><td>" +
          (m.pedidoId ? "#" + m.pedidoId : "—") +
          "</td><td>" +
          date(m.criadoEm) +
          "</td></tr>",
      );
  }
  $("#content").innerHTML = rows.length
    ? '<div class="table-scroll"><table><thead><tr>' +
      headings.map((h) => "<th>" + h + "</th>").join("") +
      "</tr></thead><tbody>" +
      rows.join("") +
      "</tbody></table></div>"
    : '<div class="empty">' +
      (search
        ? "Nenhum resultado para essa busca."
        : "Nenhum registro ainda. Comece cadastrando um produto ou pedido.") +
      "</div>";
}

function dialog(title, content, handler) {
  $("#dialog-title").textContent = title;
  const form = $("#editor-form");
  form.innerHTML =
    content +
    '<p class="form-error" role="alert"></p><div class="form-actions"><button type="button" data-close>Voltar</button><button class="primary" type="submit">Salvar</button></div>';
  form.querySelector("[data-close]").addEventListener("click", () => {
    if (!state.busy) $("#editor").close();
  });
  form.onsubmit = async (event) => {
    event.preventDefault();
    if (state.busy) return;
    state.busy = true;
    form.querySelectorAll("button").forEach((button) => {
      button.disabled = true;
    });
    form.querySelector(".form-error").textContent = "";
    try {
      await handler(new FormData(form));
      await refresh();
      $("#editor").close();
      notice("Operação concluída. O saldo e o histórico foram atualizados.");
    } catch (error) {
      form.querySelector(".form-error").textContent = error.message;
    } finally {
      state.busy = false;
      form.querySelectorAll("button").forEach((button) => {
        button.disabled = false;
      });
    }
  };
  $("#editor").showModal();
}
$("#editor").addEventListener("cancel", (event) => {
  if (state.busy) event.preventDefault();
});

function productEditor(product) {
  dialog(
    product ? "Editar produto" : "Novo produto",
    (!product
      ? '<label>Código (SKU)<input name="sku" required minlength="2" maxlength="40" pattern="[A-Za-z0-9_-]+" placeholder="Ex.: LIV-005"></label>'
      : "") +
      '<label>Nome do produto<input name="nome" required minlength="2" maxlength="100" value="' +
      escape(product?.nome || "") +
      '"></label><div class="form-row"><label>Preço unitário (R$)<input name="preco" type="number" min="0.01" max="9999999999.99" step="0.01" required value="' +
      (product?.preco || "") +
      '"></label>' +
      (!product
        ? '<label>Saldo inicial<input name="estoque" type="number" min="0" max="1000000" step="1" required value="0"></label>'
        : '<p class="muted">Para alterar o saldo, use Repor. Saídas são registradas por pedidos.</p>') +
      "</div>",
    async (fields) => {
      const body = {
        nome: fields.get("nome").trim(),
        preco: fields.get("preco"),
      };
      if (!product)
        Object.assign(body, {
          sku: fields.get("sku").trim(),
          estoque: Number(fields.get("estoque")),
        });
      await api("produtos" + (product ? "/" + product.id : ""), {
        method: product ? "PUT" : "POST",
        body: JSON.stringify(body),
      });
    },
  );
}

function replenishEditor(product) {
  dialog(
    "Repor " + product.nome,
    '<p class="muted">Saldo atual: ' +
      product.estoque +
      ' unidades. Esta entrada ficará no histórico.</p><label>Quantidade recebida<input name="quantidade" type="number" min="1" max="1000000" step="1" required value="1"></label>',
    (fields) =>
      api("produtos/" + product.id + "/reposicoes", {
        method: "POST",
        body: JSON.stringify({ quantidade: Number(fields.get("quantidade")) }),
      }),
  );
}

function orderEditor() {
  if (!state.produtos.length) {
    notice("Cadastre um produto antes de criar um pedido.", true);
    return;
  }
  dialog(
    "Novo pedido",
    '<label>Cliente<input name="cliente" minlength="2" maxlength="80" required placeholder="Use um nome fictício"></label><div id="order-items"></div><button type="button" id="add-item">+ Adicionar produto</button><p class="order-total" id="order-total"></p><p class="muted">O pedido será salvo como rascunho. A confirmação verifica e baixa o estoque.</p>',
    async (fields) => {
      const ids = fields.getAll("produto"),
        quantities = fields.getAll("quantidade");
      await api("pedidos", {
        method: "POST",
        body: JSON.stringify({
          cliente: fields.get("cliente").trim(),
          itens: ids.map((id, index) => ({
            produtoId: Number(id),
            quantidade: Number(quantities[index]),
          })),
        }),
      });
    },
  );
  const add = () => {
    if ($("#order-items").children.length >= 30) return;
    const row = document.createElement("div");
    row.className = "order-item";
    row.innerHTML =
      '<label>Produto<select name="produto">' +
      state.produtos
        .map(
          (p) =>
            '<option value="' +
            p.id +
            '">' +
            escape(p.nome) +
            " · " +
            p.estoque +
            " un.</option>",
        )
        .join("") +
      '</select></label><label>Qtd.<input name="quantidade" type="number" min="1" max="9999" step="1" value="1" required></label><button type="button" aria-label="Remover item">×</button>';
    row.querySelector("button").addEventListener("click", () => {
      if ($("#order-items").children.length > 1) {
        row.remove();
        total();
      }
    });
    row.addEventListener("input", total);
    $("#order-items").append(row);
    total();
  };
  function total() {
    let sum = 0;
    $("#order-items")
      .querySelectorAll(".order-item")
      .forEach((row) => {
        const product = state.produtos.find(
          (p) => p.id === Number(row.querySelector("select").value),
        );
        sum +=
          Number(product.preco) * Number(row.querySelector("input").value || 0);
      });
    $("#order-total").textContent = money(sum);
  }
  $("#add-item").addEventListener("click", add);
  add();
}

$("#content").addEventListener("click", async (event) => {
  const button = event.target.closest("[data-action]");
  if (!button || state.busy) return;
  const id = Number(button.dataset.id),
    action = button.dataset.action;
  if (action === "edit")
    return productEditor(state.produtos.find((p) => p.id === id));
  if (action === "replenish")
    return replenishEditor(state.produtos.find((p) => p.id === id));
  const order = state.pedidos.find((p) => p.id === id);
  const title =
    action === "confirm" ? "Confirmar pedido #" + id : "Cancelar pedido #" + id;
  const message =
    action === "confirm"
      ? "O saldo de todos os itens será verificado e debitado em uma única operação."
      : order.status === "CONFIRMADO"
        ? "Os itens deste pedido serão devolvidos ao estoque uma única vez."
        : "Este rascunho será cancelado. O saldo não será alterado.";
  dialog(
    title,
    "<p>" +
      escape(message) +
      '</p><p class="muted">' +
      escape(order.cliente) +
      " · " +
      money(order.total) +
      "</p>",
    () =>
      api(
        "pedidos/" +
          id +
          "/" +
          (action === "confirm" ? "confirmacao" : "cancelamento"),
        { method: "POST" },
      ),
  );
  $("#editor-form").querySelector('[type="submit"]').textContent =
    action === "confirm" ? "Confirmar pedido" : "Cancelar pedido";
});
