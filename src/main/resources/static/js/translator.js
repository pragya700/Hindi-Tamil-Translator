// JavaScript for the translator page (home.html).
// It sends the text and the chosen language direction to POST /api/translate and shows
// the result without reloading the page.
// Language codes: "hi" = Hindi, "ta" = Tamil (the same codes the Java backend uses).

(function () {
    var LANGUAGES = {
        hi: {
            name: "Hindi",
            placeholder: "Enter Hindi text here (यहाँ हिंदी टेक्स्ट लिखें...)",
            examples: [
                "नमस्ते, आप कैसे हैं?",
                "मैं तमिल सीख रहा हूँ।",
                "आज मौसम अच्छा है।",
                "मुझे तमिल सीखना है।"
            ]
        },
        ta: {
            name: "Tamil",
            placeholder: "Enter Tamil text here (இங்கே தமிழ் உரையை உள்ளிடவும்...)",
            examples: [
                "வணக்கம், நீங்கள் எப்படி இருக்கிறீர்கள்?",
                "நான் தமிழ் கற்றுக்கொள்கிறேன்.",
                "இன்று வானிலை நன்றாக இருக்கிறது.",
                "எனக்கு தமிழ் கற்றுக்கொள்ள வேண்டும்."
            ]
        }
    };

    var inputEl = document.getElementById("hindiInput");   // input box (Hindi OR Tamil)
    var outputEl = document.getElementById("tamilOutput"); // output box (Tamil OR Hindi)
    var inputHeading = document.getElementById("inputHeading");
    var outputHeading = document.getElementById("outputHeading");
    var messageEl = document.getElementById("message");
    var unknownEl = document.getElementById("unknownInfo");
    var loadingEl = document.getElementById("loading");
    var sourceSelect = document.getElementById("sourceLang");
    var targetSelect = document.getElementById("targetLang");
    var exampleList = document.getElementById("exampleList");
    var translateBtn = document.getElementById("translateBtn");
    var clearBtn = document.getElementById("clearBtn");
    var copyBtn = document.getElementById("copyBtn");
    var swapBtn = document.getElementById("swapBtn");

    var currentTranslation = ""; // the last result (empty if none)

    // CSRF token from the <meta> tags in home.html
    var csrfToken = document.querySelector('meta[name="_csrf"]').content;
    var csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    function otherLanguage(code) {
        return code === "hi" ? "ta" : "hi";
    }

    function showMessage(text, type) {
        messageEl.textContent = text;
        messageEl.className = "alert " + type;
    }

    function hideMessage() {
        messageEl.className = "alert hidden";
    }

    function setLoading(isLoading) {
        loadingEl.classList.toggle("hidden", !isLoading);
        translateBtn.disabled = isLoading;
    }

    function resetOutput() {
        currentTranslation = "";
        outputEl.textContent = LANGUAGES[targetSelect.value].name + " translation will appear here...";
        outputEl.classList.add("placeholder");
        unknownEl.classList.add("hidden");
    }

    // Updates headings, placeholder, font language and example buttons to match the direction.
    function updateDirectionUi() {
        var source = LANGUAGES[sourceSelect.value];
        var target = LANGUAGES[targetSelect.value];

        inputHeading.textContent = source.name + " Input";
        outputHeading.textContent = target.name + " Translation";
        inputEl.placeholder = source.placeholder;
        inputEl.lang = sourceSelect.value;
        outputEl.lang = targetSelect.value;

        exampleList.textContent = "";
        source.examples.forEach(function (sentence) {
            var chip = document.createElement("button");
            chip.type = "button";
            chip.className = "chip";
            chip.lang = sourceSelect.value;
            chip.textContent = sentence;
            chip.addEventListener("click", function () {
                inputEl.value = sentence;
                hideMessage();
                inputEl.focus();
            });
            exampleList.appendChild(chip);
        });
    }

    // Only two languages exist, so the two dropdowns always stay different from each other.
    sourceSelect.addEventListener("change", function () {
        targetSelect.value = otherLanguage(sourceSelect.value);
        updateDirectionUi();
        resetOutput(); // an old result no longer matches the new direction
        hideMessage();
    });

    targetSelect.addEventListener("change", function () {
        sourceSelect.value = otherLanguage(targetSelect.value);
        updateDirectionUi();
        resetOutput();
        hideMessage();
    });

    function swapLanguages() {
        var hadTranslation = currentTranslation !== "";
        var oldSource = sourceSelect.value;
        sourceSelect.value = targetSelect.value;
        targetSelect.value = oldSource;

        // The old output was written in the new source language, so it becomes the new input.
        // We do NOT translate it automatically: the output is cleared until Translate is clicked.
        if (hadTranslation) {
            inputEl.value = currentTranslation;
        }
        updateDirectionUi();
        resetOutput();

        if (hadTranslation) {
            showMessage("Languages swapped. The previous translation is now the input. Click Translate to translate it.", "info");
        } else {
            showMessage("Languages swapped. Enter text and click Translate.", "info");
        }
    }

    function translate() {
        hideMessage();
        var text = inputEl.value;

        if (text.trim() === "") {
            showMessage("Please enter some " + LANGUAGES[sourceSelect.value].name + " text to translate.", "warning");
            return;
        }

        var sentSource = sourceSelect.value;
        var sentTarget = targetSelect.value;
        setLoading(true);

        var headers = { "Content-Type": "application/json" };
        headers[csrfHeader] = csrfToken;

        fetch("/api/translate", {
            method: "POST",
            headers: headers,
            body: JSON.stringify({
                text: text,
                sourceLanguage: sentSource,
                targetLanguage: sentTarget
            })
        })
            .then(function (response) {
                // Not logged in (session expired): go to the login page
                if (response.status === 401 || response.redirected) {
                    window.location.href = "/login";
                    return null;
                }
                return response.json()
                    .catch(function () { return {}; })
                    .then(function (data) { return { ok: response.ok, status: response.status, data: data }; });
            })
            .then(function (result) {
                if (result === null) { return; }

                // The user changed the direction while we were waiting: ignore this answer.
                if (sourceSelect.value !== sentSource || targetSelect.value !== sentTarget) { return; }

                if (!result.ok) {
                    if (result.status === 403) {
                        showMessage("Your session may have expired. Please refresh the page and log in again.", "error");
                    } else {
                        showMessage(result.data.message || "Something went wrong. Please try again.", "error");
                    }
                    return;
                }

                // textContent (not innerHTML) keeps the output safe from HTML injection
                currentTranslation = result.data.translation;
                outputEl.textContent = currentTranslation;
                outputEl.classList.remove("placeholder");

                var unknown = result.data.unknownWords || [];
                if (unknown.length > 0) {
                    unknownEl.textContent = "Not in the demo dictionary (left unchanged): " + unknown.join(", ");
                    unknownEl.classList.remove("hidden");
                } else {
                    unknownEl.classList.add("hidden");
                }
            })
            .catch(function () {
                showMessage("Could not reach the server. Please check your connection and try again.", "error");
            })
            .finally(function () {
                setLoading(false);
            });
    }

    function clearAll() {
        inputEl.value = "";
        resetOutput();
        hideMessage();
        inputEl.focus();
    }

    function fallbackCopy(text) {
        var temp = document.createElement("textarea");
        temp.value = text;
        document.body.appendChild(temp);
        temp.select();
        var success = false;
        try { success = document.execCommand("copy"); } catch (e) { success = false; }
        document.body.removeChild(temp);
        return success;
    }

    function reportCopy(copied) {
        showMessage(copied ? "Translation copied to clipboard." : "Copy failed. Please select the text and copy it manually.",
                    copied ? "success" : "error");
    }

    function copyTranslation() {
        if (currentTranslation === "") {
            showMessage("There is no translation to copy yet.", "warning");
            return;
        }
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(currentTranslation)
                .then(function () { reportCopy(true); })
                .catch(function () { reportCopy(fallbackCopy(currentTranslation)); });
        } else {
            reportCopy(fallbackCopy(currentTranslation));
        }
    }

    translateBtn.addEventListener("click", translate);
    clearBtn.addEventListener("click", clearAll);
    copyBtn.addEventListener("click", copyTranslation);
    swapBtn.addEventListener("click", swapLanguages);

    updateDirectionUi();
    resetOutput();
})();
