function toggleTheme() {
    let theme = window.localStorage.getItem("theme");
    setTheme(theme === "light" ? "dark" : "light");
}

function setTheme(theme) {
    window.localStorage.setItem("theme", theme);

    if (theme === "dark") document.body.setAttribute("data-theme", "dark");
    else document.body.setAttribute("data-theme", "light");

    document.getElementById("theme-toggle").textContent = `[theme:${theme}]`;
}

setTheme(window.localStorage.getItem("theme"));