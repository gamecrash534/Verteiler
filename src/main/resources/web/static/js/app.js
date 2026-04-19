function toggleTheme() {
    const theme = window.localStorage.getItem("theme");
    setTheme(theme === "light" ? "dark" : "light");
}

function setTheme(theme) {
    if (!theme) theme = window.matchMedia?.('(prefers-color-scheme: dark)').matches ? "dark" : "light";
    window.localStorage.setItem("theme", theme);
    document.body.dataset.theme = theme;
    document.getElementById("theme-toggle").textContent = `[theme:${theme}]`;
}

setTheme(window.localStorage.getItem("theme"));