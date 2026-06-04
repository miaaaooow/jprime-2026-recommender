const state = {
    users: [],
    userProfiles: new Map(),
    articles: [],
    selectedUserId: null,
    recommendationLimit: 6
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
        const [users, articles] = await Promise.all([
            fetchJson("/api/users"),
            fetchJson("/api/articles")
        ]);

        state.users = users;
        state.articles = [...articles].sort(sortByCreatedAtDescending);

        elements.userCount.textContent = String(state.users.length);
        elements.articleCount.textContent = String(state.articles.length);

        renderArticles();

        if (state.users.length === 0) {
            renderEmptyUsers();
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

    renderUserCards();
    renderProfile(user, profile);
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
    elements.profileCardCount.textContent = "0";
    elements.topicCount.textContent = "0";
    elements.tagCount.textContent = "0";
}

function renderGlobalError(error) {
    const message = extractErrorMessage(error);
    elements.profileSummary.textContent = message;
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

function renderProfile(user, profile) {
    elements.topicCount.textContent = String(Object.keys(profile.topicWeights ?? {}).length);
    elements.tagCount.textContent = String(Object.keys(profile.tagWeights ?? {}).length);

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
        </div>
    `;

    renderWeights(elements.topicWeights, profile.topicWeights, "No weighted topics yet.");
    renderWeights(elements.tagWeights, profile.tagWeights, "No weighted tags yet.");
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

async function fetchJson(url) {
    const response = await fetch(url, {
        headers: {
            Accept: "application/json"
        }
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

    return response.json();
}

function sortByCreatedAtDescending(left, right) {
    const leftTime = left.createdAt ? Date.parse(left.createdAt) : 0;
    const rightTime = right.createdAt ? Date.parse(right.createdAt) : 0;
    return rightTime - leftTime;
}

function escapeHtml(value) {
    return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
}
