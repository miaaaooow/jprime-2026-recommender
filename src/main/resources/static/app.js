const state = {
    users: [],
    userProfiles: new Map(),
    userSubscriptions: new Map(),
    publishers: [],
    articles: [],
    selectedUserId: null,
    recommendationLimit: 6,
    pendingPublisherId: null,
    subscriptionErrorMessage: null
};

const elements = {
    userCount: document.getElementById("userCount"),
    articleCount: document.getElementById("articleCount"),
    selectedUserName: document.getElementById("selectedUserName"),
    userSelect: document.getElementById("userSelect"),
    profileSummary: document.getElementById("profileSummary"),
    topicWeights: document.getElementById("topicWeights"),
    tagWeights: document.getElementById("tagWeights"),
    topicCount: document.getElementById("topicCount"),
    tagCount: document.getElementById("tagCount"),
    publisherCount: document.getElementById("publisherCount"),
    subscriptionStatus: document.getElementById("subscriptionStatus"),
    publishers: document.getElementById("publishers"),
    userCards: document.getElementById("userCards"),
    profileCardCount: document.getElementById("profileCardCount"),
    articles: document.getElementById("articles"),
    recommendationMeta: document.getElementById("recommendationMeta"),
    recommendations: document.getElementById("recommendations")
};

document.addEventListener("DOMContentLoaded", () => {
    bindEvents();
    void initializeDashboard();
});

function bindEvents() {
    elements.userSelect.addEventListener("change", async (event) => {
        const userId = Number.parseInt(event.target.value, 10);
        if (Number.isNaN(userId)) {
            return;
        }
        await selectUser(userId);
    });
}

async function initializeDashboard() {
    renderLoadingState();

    try {
        const [users, publishers] = await Promise.all([
            fetchJson("/api/users"),
            fetchJson("/api/publishers")
        ]);

        state.users = users;
        state.publishers = [...publishers].sort(sortByName);

        elements.userCount.textContent = String(state.users.length);
        elements.publisherCount.textContent = String(state.publishers.length);

        if (state.users.length === 0) {
            await loadArticles();
            renderEmptyUsers();
            renderPublisherManagement(null, []);
            return;
        }

        await loadProfiles(state.users);
        renderUserSelector();
        renderUserCards();
        await selectUser(state.users[0].id);
    } catch (error) {
        renderGlobalError(error);
    }
}

async function loadProfiles(users) {
    const profileResults = await Promise.allSettled(
            users.map(async (user) => [user.id, await fetchJson(`/api/users/${user.id}/profile`)])
    );

    profileResults.forEach((result) => {
        if (result.status === "fulfilled") {
            const [userId, profile] = result.value;
            state.userProfiles.set(userId, profile);
        }
    });
}

async function selectUser(userId) {
    state.selectedUserId = userId;
    state.subscriptionErrorMessage = null;
    elements.userSelect.value = String(userId);

    const user = state.users.find((candidate) => candidate.id === userId);
    elements.selectedUserName.textContent = user ? user.username : "Unknown";

    let profile = state.userProfiles.get(userId);
    if (!profile) {
        try {
            profile = await fetchJson(`/api/users/${userId}/profile`);
            state.userProfiles.set(userId, profile);
        } catch (error) {
            renderProfileError(error);
            return;
        }
    }

    let subscriptions = state.userSubscriptions.get(userId);
    if (!subscriptions) {
        try {
            subscriptions = await fetchJson(`/api/users/${userId}/subscriptions`);
            state.userSubscriptions.set(userId, subscriptions);
        } catch (error) {
            subscriptions = [];
            state.userSubscriptions.set(userId, subscriptions);
        }
    }

    renderUserCards();
    renderProfile(user, profile, subscriptions);
    renderPublisherManagement(user, subscriptions);
    await loadArticles(userId);
    await renderRecommendations(userId);
}

async function renderRecommendations(userId) {
    elements.recommendationMeta.innerHTML = "";
    elements.recommendations.innerHTML = "Loading recommendations...";
    elements.recommendations.className = "recommendations empty-state";

    try {
        const recommendations = await fetchJson(
                `/api/users/${userId}/recommendations?limit=${state.recommendationLimit}`
        );
        const user = state.users.find((candidate) => candidate.id === userId);
        const profile = state.userProfiles.get(userId);
        renderRecommendationMeta(user, profile, recommendations);

        if (recommendations.length === 0) {
            elements.recommendations.textContent = "No recommendations yet. Record some reads, likes, or shares first.";
            return;
        }

        elements.recommendations.className = "recommendations";
        elements.recommendations.innerHTML = recommendations
                .map((recommendation) => recommendationCard(recommendation))
                .join("");
    } catch (error) {
        elements.recommendationMeta.className = "recommendation-meta empty-state";
        elements.recommendationMeta.textContent = "Recommendation preview could not be loaded.";
        elements.recommendations.className = "recommendations empty-state";
        elements.recommendations.textContent = extractErrorMessage(error);
    }
}

function renderLoadingState() {
    elements.profileSummary.textContent = "Loading users and profile data...";
    elements.topicWeights.textContent = "Loading topics...";
    elements.tagWeights.textContent = "Loading tags...";
    elements.subscriptionStatus.textContent = "Loading publishers...";
    elements.publishers.textContent = "Loading publishers...";
    elements.userCards.textContent = "Loading users...";
    elements.articles.textContent = "Loading articles...";
    elements.recommendations.textContent = "Select a user to preview recommendations.";
}

function renderEmptyUsers() {
    elements.selectedUserName.textContent = "None";
    elements.userSelect.innerHTML = "<option>No users available</option>";
    elements.userSelect.disabled = true;
    elements.profileSummary.textContent = "No users found. Create a user through the API first.";
    elements.topicWeights.textContent = "No weighted topics yet.";
    elements.tagWeights.textContent = "No weighted tags yet.";
    elements.userCards.textContent = "No users available.";
    elements.recommendationMeta.textContent = "No user selected.";
    elements.recommendations.textContent = "No recommendations available.";
    elements.subscriptionStatus.textContent = "Create or seed a user to manage publisher subscriptions.";
    elements.profileCardCount.textContent = "0";
    elements.topicCount.textContent = "0";
    elements.tagCount.textContent = "0";
}

async function loadArticles(userId = null) {
    const query = userId == null ? "" : `?userId=${userId}`;
    state.articles = [...await fetchJson(`/api/articles${query}`)].sort(sortByCreatedAtDescending);
    elements.articleCount.textContent = String(state.articles.length);
    renderArticles();
}

function renderGlobalError(error) {
    const message = extractErrorMessage(error);
    elements.profileSummary.textContent = message;
    elements.subscriptionStatus.textContent = message;
    elements.publishers.textContent = message;
    elements.userCards.textContent = message;
    elements.articles.textContent = message;
    elements.recommendations.textContent = message;
}

function renderProfileError(error) {
    const message = extractErrorMessage(error);
    elements.profileSummary.textContent = message;
    elements.topicWeights.textContent = message;
    elements.tagWeights.textContent = message;
}

function renderUserSelector() {
    elements.userSelect.disabled = false;
    elements.userSelect.innerHTML = state.users
            .map((user) => `<option value="${user.id}">${escapeHtml(user.username)}</option>`)
            .join("");
}

function renderProfile(user, profile, subscriptions) {
    elements.topicCount.textContent = String(Object.keys(profile.topicWeights ?? {}).length);
    elements.tagCount.textContent = String(Object.keys(profile.tagWeights ?? {}).length);
    const subscriptionChips = subscriptions && subscriptions.length > 0
            ? subscriptions.map((subscription) => chip(subscription.publisherName, "is-gold")).join("")
            : chip("no subscriptions");

    const summaryBits = [
        `<strong>${escapeHtml(user.username)}</strong>`,
        escapeHtml(user.email),
        `model: <strong>${escapeHtml(profile.similarityModel)}</strong>`
    ];

    elements.profileSummary.className = "profile-summary";
    elements.profileSummary.innerHTML = `
        <div class="stack">
            <div>${summaryBits.join(" · ")}</div>
            <div class="inline-stat-list">
                ${chip(`reads ${profile.readArticleCount}`, "is-accent")}
                ${chip(`likes ${profile.likedArticleCount}`, "is-accent")}
                ${chip(`shares ${profile.sharedArticleCount}`, "is-accent")}
                ${chip(`updated ${formatTimestamp(profile.updatedAt)}`, "is-gold")}
            </div>
            <div class="stack">
                <div class="muted">Subscribed publishers</div>
                <div class="chip-row">${subscriptionChips}</div>
            </div>
        </div>
    `;

    renderWeights(elements.topicWeights, profile.topicWeights, "No weighted topics yet.");
    renderWeights(elements.tagWeights, profile.tagWeights, "No weighted tags yet.");
}

function renderPublisherManagement(user, subscriptions) {
    elements.publisherCount.textContent = String(state.publishers.length);

    if (state.publishers.length === 0) {
        elements.subscriptionStatus.className = "profile-summary empty-state";
        elements.subscriptionStatus.textContent = "No publishers available.";
        elements.publishers.className = "publisher-grid empty-state";
        elements.publishers.textContent = "No publishers available.";
        return;
    }

    const subscribedPublisherIds = new Set((subscriptions ?? []).map((subscription) => subscription.publisherId));

    if (!user) {
        elements.subscriptionStatus.className = "profile-summary empty-state";
        elements.subscriptionStatus.textContent = state.users.length === 0
                ? "Create or seed a user to manage publisher subscriptions."
                : "Select a user to manage publisher subscriptions.";
    } else {
        const errorMessage = state.subscriptionErrorMessage
                ? `<div class="status-error">${escapeHtml(state.subscriptionErrorMessage)}</div>`
                : "";

        elements.subscriptionStatus.className = "profile-summary";
        elements.subscriptionStatus.innerHTML = `
            <div class="stack">
                <div>
                    Managing subscriptions for <strong>${escapeHtml(user.username)}</strong>.
                </div>
                <div class="inline-stat-list">
                    ${chip(`${subscribedPublisherIds.size} active`, "is-gold")}
                    ${chip(`${state.publishers.length} publishers`)}
                </div>
                ${errorMessage}
            </div>
        `;
    }

    elements.publishers.className = "publisher-grid";
    elements.publishers.innerHTML = state.publishers
            .map((publisher) => publisherCard(publisher, {
                canManage: user != null,
                isSubscribed: subscribedPublisherIds.has(publisher.id),
                isPending: state.pendingPublisherId === publisher.id
            }))
            .join("");

    elements.publishers.querySelectorAll("[data-subscription-action]").forEach((button) => {
        button.addEventListener("click", async () => {
            const publisherId = Number.parseInt(button.dataset.publisherId, 10);
            const isSubscribed = button.dataset.subscribed === "true";

            if (Number.isNaN(publisherId)) {
                return;
            }

            await updateSubscription(publisherId, isSubscribed);
        });
    });
}

function renderWeights(target, weights, emptyMessage) {
    const entries = Object.entries(weights ?? {})
            .sort((left, right) => right[1] - left[1] || left[0].localeCompare(right[0]));

    if (entries.length === 0) {
        target.className = "weight-list empty-state";
        target.textContent = emptyMessage;
        return;
    }

    target.className = "weight-list";
    target.innerHTML = entries
            .map(([label, value]) => `
                <div class="weight-item">
                    <span class="weight-item-label">${escapeHtml(label)}</span>
                    <span class="weight-item-value">${formatWeight(value)}</span>
                </div>
            `)
            .join("");
}

function renderUserCards() {
    elements.profileCardCount.textContent = String(state.users.length);

    if (state.users.length === 0) {
        elements.userCards.className = "user-cards empty-state";
        elements.userCards.textContent = "No users available.";
        return;
    }

    elements.userCards.className = "user-cards";
    elements.userCards.innerHTML = state.users.map((user) => userCard(user)).join("");

    elements.userCards.querySelectorAll("[data-user-id]").forEach((button) => {
        button.addEventListener("click", async () => {
            const userId = Number.parseInt(button.dataset.userId, 10);
            await selectUser(userId);
        });
    });
}

function renderRecommendationMeta(user, profile, recommendations) {
    const matchedTopicCount = recommendations.reduce(
            (total, recommendation) => total + recommendation.matchedTopics.length,
            0
    );
    const matchedTagCount = recommendations.reduce(
            (total, recommendation) => total + recommendation.matchedTags.length,
            0
    );

    elements.recommendationMeta.className = "recommendation-meta";
    elements.recommendationMeta.innerHTML = `
        <div class="stack">
            <div>
                Previewing <strong>${escapeHtml(user.username)}</strong> with
                <strong>${escapeHtml(profile.similarityModel)}</strong>.
            </div>
            <div class="inline-stat-list">
                ${chip(`${recommendations.length} articles`, "is-gold")}
                ${chip(`${matchedTopicCount} matched topics`)}
                ${chip(`${matchedTagCount} matched tags`)}
            </div>
        </div>
    `;
}

function renderArticles() {
    if (state.articles.length === 0) {
        elements.articles.className = "article-grid empty-state";
        elements.articles.textContent = "No articles available.";
        return;
    }

    elements.articles.className = "article-grid";
    elements.articles.innerHTML = state.articles.map(articleCard).join("");
}

function userCard(user) {
    const profile = state.userProfiles.get(user.id);
    const isSelected = state.selectedUserId === user.id ? " is-selected" : "";
    const topicChips = topWeightLabels(profile?.topicWeights, "No topics yet.");
    const tagChips = topWeightLabels(profile?.tagWeights, "No tags yet.");

    return `
        <button class="user-card${isSelected}" type="button" data-user-id="${user.id}">
            <div>
                <h4>${escapeHtml(user.username)}</h4>
                <div class="muted">${escapeHtml(user.email)}</div>
            </div>
            <div class="stack">
                <div class="muted">Top topics</div>
                <div class="chip-row">${topicChips}</div>
            </div>
            <div class="stack">
                <div class="muted">Top tags</div>
                <div class="chip-row">${tagChips}</div>
            </div>
        </button>
    `;
}

async function updateSubscription(publisherId, isSubscribed) {
    const userId = state.selectedUserId;
    if (userId == null || state.pendingPublisherId != null) {
        return;
    }

    const user = state.users.find((candidate) => candidate.id === userId);
    if (!user) {
        return;
    }

    state.pendingPublisherId = publisherId;
    state.subscriptionErrorMessage = null;
    renderPublisherManagement(user, state.userSubscriptions.get(userId) ?? []);

    try {
        const url = `/api/users/${userId}/subscriptions/${publisherId}`;

        if (isSubscribed) {
            await sendRequest(url, {method: "DELETE"});
        } else {
            await fetchJson(url, {method: "POST"});
        }

        const subscriptions = await fetchJson(`/api/users/${userId}/subscriptions`);
        state.userSubscriptions.set(userId, subscriptions);

        renderProfile(user, state.userProfiles.get(userId), subscriptions);
        await Promise.all([
            loadArticles(userId),
            renderRecommendations(userId)
        ]);
    } catch (error) {
        state.subscriptionErrorMessage = extractErrorMessage(error);
    } finally {
        state.pendingPublisherId = null;
        renderPublisherManagement(user, state.userSubscriptions.get(userId) ?? []);
    }
}

function recommendationCard(recommendation) {
    const article = state.articles.find((candidate) => candidate.id === recommendation.articleId);
    const matchedTopics = recommendation.matchedTopics.length > 0
            ? recommendation.matchedTopics.map((topic) => chip(topic, "is-accent")).join("")
            : chip("serendipity", "is-gold");
    const matchedTags = recommendation.matchedTags.length > 0
            ? recommendation.matchedTags.map((tag) => chip(tag)).join("")
            : chip("no matched tags");

    return `
        <article class="recommendation-card">
            <div class="section-title-row">
                <h3>${escapeHtml(recommendation.title)}</h3>
                <span class="score-badge">${recommendation.score.toFixed(3)}</span>
            </div>
            <div class="card-meta">
                <span>${visibilityLabel(recommendation.accessLevel)}</span>
                <span>${escapeHtml(article?.publisherName ?? "Unknown publisher")}</span>
                <span>${escapeHtml(article?.authorName ?? "Unknown author")}</span>
            </div>
            <p class="card-copy">${escapeHtml(snippet(article?.content ?? "", 170))}</p>
            <div class="stack">
                <div class="muted">Matched topics</div>
                <div class="chip-row">${matchedTopics}</div>
            </div>
            <div class="stack">
                <div class="muted">Matched tags</div>
                <div class="chip-row">${matchedTags}</div>
            </div>
        </article>
    `;
}

function articleCard(article) {
    return `
        <article class="article-card">
            <h3>${escapeHtml(article.title)}</h3>
            <div class="card-meta">
                <span>${visibilityLabel(article.accessLevel)}</span>
                <span>${escapeHtml(article.publisherName ?? "Unknown publisher")}</span>
                <span>${escapeHtml(article.authorName ?? "Unknown author")}</span>
                <span>${formatTimestamp(article.createdAt)}</span>
            </div>
            <p class="card-copy">${escapeHtml(snippet(article.content, 210))}</p>
            <div class="stack">
                <div class="muted">Topics</div>
                <div class="chip-row">${renderChipRow(article.topics, "is-accent", "No topics")}</div>
            </div>
            <div class="stack">
                <div class="muted">Tags</div>
                <div class="chip-row">${renderChipRow(article.tags, "", "No tags")}</div>
            </div>
        </article>
    `;
}

function publisherCard(publisher, options) {
    const articles = publisher.articles ?? [];
    const authors = publisher.authors ?? [];
    const publicArticleCount = articles.filter((article) => article.accessLevel !== "SUBSCRIBERS_ONLY").length;
    const subscriberOnlyArticleCount = articles.length - publicArticleCount;
    const authorChips = authors.length > 0
            ? authors
                    .slice(0, 4)
                    .map((author) => chip(`${author.name} ${formatRating(author.internalRating)}`, "is-accent"))
                    .join("")
            : chip("No authors");
    const buttonLabel = options.canManage
            ? (options.isPending
                ? (options.isSubscribed ? "Unsubscribing..." : "Subscribing...")
                : (options.isSubscribed ? "Unsubscribe" : "Subscribe"))
            : "Select a user";
    const buttonClasses = options.isSubscribed ? "action-button is-secondary" : "action-button";

    return `
        <article class="publisher-card${options.isSubscribed ? " is-subscribed" : ""}">
            <div class="publisher-card-header">
                <div>
                    <h3>${escapeHtml(publisher.name)}</h3>
                    <div class="muted">${articles.length} articles · ${authors.length} authors</div>
                </div>
                ${options.isSubscribed ? chip("Subscribed", "is-gold") : chip("Not subscribed")}
            </div>
            <div class="inline-stat-list">
                ${chip(`${publicArticleCount} public`, "is-accent")}
                ${chip(`${subscriberOnlyArticleCount} subscribers only`, "is-gold")}
            </div>
            <div class="stack">
                <div class="muted">Authors</div>
                <div class="chip-row">${authorChips}</div>
            </div>
            <div class="publisher-actions">
                <button
                    class="${buttonClasses}"
                    type="button"
                    data-subscription-action="toggle"
                    data-publisher-id="${publisher.id}"
                    data-subscribed="${options.isSubscribed}"
                    ${(!options.canManage || state.pendingPublisherId != null) ? "disabled" : ""}
                >
                    ${buttonLabel}
                </button>
            </div>
        </article>
    `;
}

function topWeightLabels(weights, fallbackLabel) {
    const entries = Object.entries(weights ?? {})
            .sort((left, right) => right[1] - left[1] || left[0].localeCompare(right[0]))
            .slice(0, 4);

    if (entries.length === 0) {
        return chip(fallbackLabel);
    }

    return entries
            .map(([label, value]) => chip(`${label} ${formatWeight(value)}`))
            .join("");
}

function renderChipRow(values, className, fallbackLabel) {
    if (!values || values.length === 0) {
        return chip(fallbackLabel);
    }

    return values.map((value) => chip(value, className)).join("");
}

function chip(value, className = "") {
    const classes = ["chip", className].filter(Boolean).join(" ");
    return `<span class="${classes}">${escapeHtml(value)}</span>`;
}

function formatWeight(value) {
    return Number(value).toFixed(3);
}

function formatRating(value) {
    return Number(value).toFixed(1);
}

function formatTimestamp(value) {
    if (!value) {
        return "unknown";
    }

    const date = new Date(value);
    return date.toLocaleString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

function visibilityLabel(accessLevel) {
    return accessLevel === "SUBSCRIBERS_ONLY" ? "subscribers only" : "public";
}

function snippet(text, maxLength) {
    if (!text) {
        return "No content preview.";
    }

    return text.length > maxLength ? `${text.slice(0, maxLength - 1).trimEnd()}…` : text;
}

function extractErrorMessage(error) {
    if (error instanceof Error) {
        return error.message;
    }
    return "The dashboard request failed.";
}

async function fetchJson(url, options = {}) {
    const response = await sendRequest(url, options);

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

async function sendRequest(url, options = {}) {
    const headers = new Headers(options.headers ?? {});
    if (!headers.has("Accept")) {
        headers.set("Accept", "application/json");
    }

    const response = await fetch(url, {
        ...options,
        headers
    });

    if (!response.ok) {
        let message = `Request failed with status ${response.status}.`;

        try {
            const body = await response.json();
            if (body.message) {
                message = body.message;
            }
        } catch (ignored) {
            message = response.statusText || message;
        }

        throw new Error(message);
    }

    return response;
}

function sortByCreatedAtDescending(left, right) {
    const leftTime = left.createdAt ? Date.parse(left.createdAt) : 0;
    const rightTime = right.createdAt ? Date.parse(right.createdAt) : 0;
    return rightTime - leftTime;
}

function sortByName(left, right) {
    return left.name.localeCompare(right.name);
}

function escapeHtml(value) {
    return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
}
