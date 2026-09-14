// StegaSafe Client Application Logic
document.addEventListener("DOMContentLoaded", () => {
    initTheme();
    initNavigation();
    initEncodeStudio();
    initDecodeStudio();
    initCapacityStudio();
});

/* =========================================================================
   1. Theme Management (Dark / Light)
   ========================================================================= */
function initTheme() {
    const themeBtn = document.getElementById("themeToggleBtn");
    const savedTheme = localStorage.getItem("stegasafe-theme") || "dark";
    document.documentElement.setAttribute("data-theme", savedTheme);
    updateThemeButton(savedTheme);

    if (themeBtn) {
        themeBtn.addEventListener("click", () => {
            const currentTheme = document.documentElement.getAttribute("data-theme");
            const newTheme = currentTheme === "light" ? "dark" : "light";
            document.documentElement.setAttribute("data-theme", newTheme);
            localStorage.setItem("stegasafe-theme", newTheme);
            updateThemeButton(newTheme);
        });
    }
}

function updateThemeButton(theme) {
    const themeBtn = document.getElementById("themeToggleBtn");
    if (!themeBtn) return;
    if (theme === "light") {
        themeBtn.innerHTML = `
            <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"/>
            </svg> Dark Mode`;
    } else {
        themeBtn.innerHTML = `
            <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"/>
            </svg> Light Mode`;
    }
}

/* =========================================================================
   2. Navigation & Tabs
   ========================================================================= */
function initNavigation() {
    const navButtons = document.querySelectorAll(".nav-btn");
    const tabContents = document.querySelectorAll(".tab-content");

    window.switchTab = function(tabId) {
        navButtons.forEach(btn => {
            if (btn.dataset.tab === tabId) {
                btn.classList.add("active");
            } else {
                btn.classList.remove("active");
            }
        });

        tabContents.forEach(tab => {
            if (tab.id === tabId) {
                tab.classList.add("active");
            } else {
                tab.classList.remove("active");
            }
        });

        window.scrollTo({ top: 0, behavior: "smooth" });
    };

    navButtons.forEach(btn => {
        btn.addEventListener("click", () => {
            const targetTab = btn.dataset.tab;
            switchTab(targetTab);
        });
    });

    // Check if initial tab was supplied from server or hash
    const initialTab = document.body.dataset.initialTab || "dashboard";
    if (initialTab && document.getElementById(initialTab)) {
        switchTab(initialTab);
    }
}

/* =========================================================================
   3. Encode Studio
   ========================================================================= */
function initEncodeStudio() {
    const dropzone = document.getElementById("encodeDropzone");
    const fileInput = document.getElementById("encodeFileInput");
    const previewContainer = document.getElementById("encodePreviewContainer");
    const previewImg = document.getElementById("encodePreviewImg");
    const resMeta = document.getElementById("encodeResolutionMeta");
    const maxCapMeta = document.getElementById("encodeMaxCapMeta");
    const messageInput = document.getElementById("encodeMessageInput");
    const passToggle = document.getElementById("encodePasswordToggle");
    const passGroup = document.getElementById("encodePasswordGroup");
    const passInput = document.getElementById("encodePasswordInput");
    const passConfirm = document.getElementById("encodePasswordConfirm");
    const capacityFill = document.getElementById("encodeCapacityFill");
    const capacityText = document.getElementById("encodeCapacityText");
    const encodeBtn = document.getElementById("encodeSubmitBtn");
    const resultCard = document.getElementById("encodeResultCard");
    const downloadBtn = document.getElementById("downloadEncodedBtn");
    const charCounter = document.getElementById("encodeCharCounter");
    const byteCounter = document.getElementById("encodeByteCounter");

    let currentEncodeFile = null;
    let currentCapacityInfo = null;
    let encodedBlobUrl = null;
    let encodedDownloadFilename = "stegasafe_encoded.png";

    // Setup drag and drop
    setupDropzone(dropzone, fileInput, (file) => handleEncodeFile(file));

    function handleEncodeFile(file) {
        if (!file.type.startsWith("image/")) {
            showToast("Please upload an image file (PNG, JPG, BMP)", "error");
            return;
        }

        currentEncodeFile = file;

        // Preview thumbnail
        const reader = new FileReader();
        reader.onload = (e) => {
            previewImg.src = e.target.result;
            previewContainer.classList.add("active");
        };
        reader.readAsDataURL(file);

        // Fetch capacity metadata from backend
        fetchCapacity(file, (info) => {
            currentCapacityInfo = info;
            resMeta.textContent = `${info.width} × ${info.height} px`;
            maxCapMeta.textContent = info.formattedMaxCapacity;
            updateCapacityProgress();
            checkEncodeFormValidity();
        });
    }

    // Toggle Password Box
    if (passToggle) {
        passToggle.addEventListener("change", () => {
            if (passToggle.checked) {
                passGroup.style.display = "block";
            } else {
                passGroup.style.display = "none";
                passInput.value = "";
                passConfirm.value = "";
            }
            updateCapacityProgress();
            checkEncodeFormValidity();
        });
    }

    // Message Input change listener
    if (messageInput) {
        messageInput.addEventListener("input", () => {
            const text = messageInput.value;
            const byteLength = new TextEncoder().encode(text).length;
            if (charCounter) charCounter.textContent = `${text.length} chars`;
            if (byteCounter) byteCounter.textContent = `${byteLength} bytes`;
            updateCapacityProgress();
            checkEncodeFormValidity();
        });
    }

    if (passInput) passInput.addEventListener("input", checkEncodeFormValidity);
    if (passConfirm) passConfirm.addEventListener("input", checkEncodeFormValidity);

    function updateCapacityProgress() {
        if (!currentCapacityInfo || !messageInput) return;

        const text = messageInput.value;
        const msgBytes = new TextEncoder().encode(text).length;
        const isEncrypted = passToggle && passToggle.checked;
        const maxCapacity = isEncrypted 
            ? currentCapacityInfo.maxMessageBytesEncrypted 
            : currentCapacityInfo.maxMessageBytesUnencrypted;

        if (maxCapacity <= 0) return;

        const percent = Math.min(100, Math.round((msgBytes / maxCapacity) * 100));
        capacityFill.style.width = `${percent}%`;

        if (percent > 90) {
            capacityFill.className = "progress-bar-fill danger";
        } else if (percent > 60) {
            capacityFill.className = "progress-bar-fill warning";
        } else {
            capacityFill.className = "progress-bar-fill";
        }

        capacityText.textContent = `${msgBytes.toLocaleString()} / ${maxCapacity.toLocaleString()} bytes (${percent}%)`;
    }

    function checkEncodeFormValidity() {
        if (!encodeBtn) return;
        const hasFile = currentEncodeFile !== null;
        const hasMessage = messageInput && messageInput.value.trim().length > 0;
        let passValid = true;

        if (passToggle && passToggle.checked) {
            const p1 = passInput.value;
            const p2 = passConfirm.value;
            passValid = (p1.length >= 4 && p1 === p2);
        }

        let withinCapacity = true;
        if (currentCapacityInfo && messageInput) {
            const msgBytes = new TextEncoder().encode(messageInput.value).length;
            const isEncrypted = passToggle && passToggle.checked;
            const maxCapacity = isEncrypted 
                ? currentCapacityInfo.maxMessageBytesEncrypted 
                : currentCapacityInfo.maxMessageBytesUnencrypted;
            withinCapacity = msgBytes <= maxCapacity;
        }

        encodeBtn.disabled = !(hasFile && hasMessage && passValid && withinCapacity);
    }

    // Submit Encode Request
    if (encodeBtn) {
        encodeBtn.addEventListener("click", async () => {
            if (!currentEncodeFile || !messageInput.value.trim()) return;

            if (passToggle.checked && passInput.value !== passConfirm.value) {
                showToast("Passwords do not match!", "error");
                return;
            }

            const originalBtnText = encodeBtn.innerHTML;
            encodeBtn.disabled = true;
            encodeBtn.innerHTML = `
                <svg class="spin-animation" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v4m0 8v4m8-8h-4M4 12H0"/>
                </svg> Encoding Bitstream...`;

            const formData = new FormData();
            formData.append("image", currentEncodeFile);
            formData.append("message", messageInput.value);
            if (passToggle.checked && passInput.value.trim()) {
                formData.append("password", passInput.value.trim());
            }

            try {
                const response = await fetch("/api/steganography/encode", {
                    method: "POST",
                    body: formData
                });

                if (!response.ok) {
                    const errJson = await response.json();
                    throw new Error(errJson.error || "Encoding failed.");
                }

                const blob = await response.blob();
                if (encodedBlobUrl) URL.revokeObjectURL(encodedBlobUrl);
                encodedBlobUrl = URL.createObjectURL(blob);

                // Determine filename
                const disposition = response.headers.get("Content-Disposition");
                if (disposition && disposition.includes("filename=")) {
                    const match = disposition.match(/filename="?([^"]+)"?/);
                    if (match && match[1]) encodedDownloadFilename = match[1];
                }

                // Show Result Card
                resultCard.classList.add("active");
                const resultPreview = document.getElementById("encodeResultThumb");
                if (resultPreview) resultPreview.src = encodedBlobUrl;

                const resultMeta = document.getElementById("encodeResultMeta");
                if (resultMeta) {
                    const isEnc = response.headers.get("X-StegaSafe-Encrypted") === "true";
                    resultMeta.innerHTML = `
                        <strong>Format:</strong> Lossless PNG &nbsp;|&nbsp; 
                        <strong>Size:</strong> ${(blob.size / 1024).toFixed(1)} KB &nbsp;|&nbsp; 
                        <strong>Security:</strong> ${isEnc ? '<span style="color:var(--accent-purple)">AES-256-GCM Encrypted</span>' : '<span style="color:var(--accent-green)">Plaintext LSB</span>'}
                    `;
                }

                showToast("Secret message successfully encoded into image!", "success");
                resultCard.scrollIntoView({ behavior: "smooth" });
            } catch (err) {
                showToast(err.message || "Failed to encode message.", "error");
            } finally {
                encodeBtn.disabled = false;
                encodeBtn.innerHTML = originalBtnText;
                checkEncodeFormValidity();
            }
        });
    }

    // Download Button
    if (downloadBtn) {
        downloadBtn.addEventListener("click", () => {
            if (!encodedBlobUrl) return;
            const a = document.createElement("a");
            a.href = encodedBlobUrl;
            a.download = encodedDownloadFilename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            showToast("Downloading encoded PNG image...", "info");
        });
    }

    // Presets Injection
    window.insertPreset = function(type) {
        if (!messageInput) return;
        if (type === "tamil") {
            messageInput.value = "வணக்கம் உலகம்! இது ஒரு பாதுகாப்பான ஸ்டெகனோகிராபி செய்தி. வெற்றி நிச்சயம்! 🔐🚀";
        } else if (type === "credentials") {
            messageInput.value = "DB_HOST=prod-secure.cluster.local\nDB_USER=stega_admin\nAPI_KEY=sk_live_99882200aabbccdd1122";
        } else if (type === "flag") {
            messageInput.value = "FLAG{steg4s4fe_l5b_4es256_gcm_pr0t3ct10n_v1}";
        }
        messageInput.dispatchEvent(new Event("input"));
    };
}

/* =========================================================================
   4. Decode Studio
   ========================================================================= */
function initDecodeStudio() {
    const dropzone = document.getElementById("decodeDropzone");
    const fileInput = document.getElementById("decodeFileInput");
    const previewContainer = document.getElementById("decodePreviewContainer");
    const previewImg = document.getElementById("decodePreviewImg");
    const passInput = document.getElementById("decodePasswordInput");
    const decodeBtn = document.getElementById("decodeSubmitBtn");
    const resultCard = document.getElementById("decodeResultCard");
    const outputArea = document.getElementById("decodeOutputText");
    const badgeStatus = document.getElementById("decodeBadgeStatus");
    const statsText = document.getElementById("decodeStatsText");
    const copyBtn = document.getElementById("decodeCopyBtn");
    const resetBtn = document.getElementById("decodeResetBtn");

    let currentDecodeFile = null;

    setupDropzone(dropzone, fileInput, (file) => {
        if (!file.type.startsWith("image/")) {
            showToast("Please upload an encoded image file (PNG)", "error");
            return;
        }
        currentDecodeFile = file;
        const reader = new FileReader();
        reader.onload = (e) => {
            previewImg.src = e.target.result;
            previewContainer.classList.add("active");
            resultCard.classList.remove("active");
        };
        reader.readAsDataURL(file);
        decodeBtn.disabled = false;
    });

    if (decodeBtn) {
        decodeBtn.addEventListener("click", async () => {
            if (!currentDecodeFile) {
                showToast("Please select an image to decode.", "error");
                return;
            }

            const originalText = decodeBtn.innerHTML;
            decodeBtn.disabled = true;
            decodeBtn.innerHTML = `
                <svg class="spin-animation" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v4m0 8v4m8-8h-4M4 12H0"/>
                </svg> Scanning & Extracting...`;

            const formData = new FormData();
            formData.append("image", currentDecodeFile);
            if (passInput && passInput.value.trim()) {
                formData.append("password", passInput.value.trim());
            }

            try {
                const response = await fetch("/api/steganography/decode", {
                    method: "POST",
                    body: formData
                });

                const data = await response.json();

                if (!response.ok) {
                    if (response.status === 401) {
                        passInput.focus();
                        passInput.style.borderColor = "var(--accent-red)";
                        setTimeout(() => passInput.style.borderColor = "", 3000);
                    }
                    throw new Error(data.error || "Decoding failed.");
                }

                const result = data.data;
                outputArea.value = result.message;

                if (result.encrypted) {
                    badgeStatus.className = "result-badge encrypted";
                    badgeStatus.innerHTML = `
                        <svg width="12" height="12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                        </svg> AES-256-GCM Encrypted`;
                } else {
                    badgeStatus.className = "result-badge plain";
                    badgeStatus.innerHTML = `
                        <svg width="12" height="12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 11V7a4 4 0 118 0m-4 8v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2z"/>
                        </svg> Unencrypted LSB`;
                }

                statsText.textContent = `${result.characterCount} characters | ${result.payloadSizeBytes} bytes | Extracted in ${result.extractionTimeMs}ms`;
                resultCard.classList.add("active");
                resultCard.scrollIntoView({ behavior: "smooth" });
                showToast("Secret message extracted successfully!", "success");
            } catch (err) {
                showToast(err.message || "Failed to decode image.", "error");
            } finally {
                decodeBtn.disabled = false;
                decodeBtn.innerHTML = originalText;
            }
        });
    }

    if (copyBtn) {
        copyBtn.addEventListener("click", () => {
            if (!outputArea.value) return;
            navigator.clipboard.writeText(outputArea.value).then(() => {
                showToast("Message copied to clipboard!", "success");
            }).catch(() => {
                showToast("Failed to copy to clipboard", "error");
            });
        });
    }

    if (resetBtn) {
        resetBtn.addEventListener("click", () => {
            currentDecodeFile = null;
            fileInput.value = "";
            previewImg.src = "";
            previewContainer.classList.remove("active");
            resultCard.classList.remove("active");
            if (passInput) passInput.value = "";
            outputArea.value = "";
            decodeBtn.disabled = true;
            showToast("Decode studio reset.", "info");
        });
    }
}

/* =========================================================================
   5. Standalone Capacity Analyzer Studio
   ========================================================================= */
function initCapacityStudio() {
    const dropzone = document.getElementById("capacityDropzone");
    const fileInput = document.getElementById("capacityFileInput");
    const resultPanel = document.getElementById("capacityResultPanel");
    const previewImg = document.getElementById("capacityPreviewImg");

    setupDropzone(dropzone, fileInput, (file) => {
        if (!file.type.startsWith("image/")) {
            showToast("Please upload an image file", "error");
            return;
        }

        const reader = new FileReader();
        reader.onload = (e) => {
            previewImg.src = e.target.result;
        };
        reader.readAsDataURL(file);

        fetchCapacity(file, (info) => {
            document.getElementById("capFileName").textContent = info.filename;
            document.getElementById("capFileSize").textContent = info.formattedFileSize;
            document.getElementById("capDimensions").textContent = `${info.width} × ${info.height} px`;
            document.getElementById("capTotalPixels").textContent = info.totalPixels.toLocaleString();
            document.getElementById("capRawBytes").textContent = `${info.rawCapacityBytes.toLocaleString()} bytes`;
            document.getElementById("capMaxPlain").textContent = `${info.maxMessageBytesUnencrypted.toLocaleString()} bytes`;
            document.getElementById("capMaxEnc").textContent = `${info.maxMessageBytesEncrypted.toLocaleString()} bytes`;
            document.getElementById("capFormatted").textContent = info.formattedMaxCapacity;

            resultPanel.classList.add("active");
            resultPanel.scrollIntoView({ behavior: "smooth" });
        });
    });
}

/* =========================================================================
   Helper Utilities
   ========================================================================= */
function setupDropzone(dropzone, fileInput, onFileSelected) {
    if (!dropzone || !fileInput) return;

    dropzone.addEventListener("click", () => fileInput.click());

    fileInput.addEventListener("change", () => {
        if (fileInput.files && fileInput.files[0]) {
            onFileSelected(fileInput.files[0]);
        }
    });

    ["dragenter", "dragover"].forEach(eventName => {
        dropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropzone.classList.add("dragover");
        });
    });

    ["dragleave", "drop"].forEach(eventName => {
        dropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropzone.classList.remove("dragover");
        });
    });

    dropzone.addEventListener("drop", (e) => {
        if (e.dataTransfer.files && e.dataTransfer.files[0]) {
            onFileSelected(e.dataTransfer.files[0]);
        }
    });
}

async function fetchCapacity(file, callback) {
    const formData = new FormData();
    formData.append("image", file);

    try {
        const res = await fetch("/api/steganography/capacity", {
            method: "POST",
            body: formData
        });

        const json = await res.json();
        if (res.ok && json.success) {
            callback(json.data);
        } else {
            showToast(json.error || "Failed to analyze image capacity", "error");
        }
    } catch (err) {
        showToast("Error inspecting image capacity", "error");
    }
}

window.togglePasswordVisibility = function(inputId, btn) {
    const input = document.getElementById(inputId);
    if (!input) return;
    if (input.type === "password") {
        input.type = "text";
        btn.innerHTML = `<svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l18 18"/></svg>`;
    } else {
        input.type = "password";
        btn.innerHTML = `<svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/></svg>`;
    }
};

function showToast(message, type = "info") {
    let container = document.getElementById("toastContainer");
    if (!container) {
        container = document.createElement("div");
        container.id = "toastContainer";
        container.className = "toast-container";
        document.body.appendChild(container);
    }

    const toast = document.createElement("div");
    toast.className = `toast ${type}`;

    let iconSvg = "";
    if (type === "success") {
        iconSvg = `<svg width="20" height="20" fill="none" stroke="var(--accent-green)" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>`;
    } else if (type === "error") {
        iconSvg = `<svg width="20" height="20" fill="none" stroke="var(--accent-red)" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>`;
    } else {
        iconSvg = `<svg width="20" height="20" fill="none" stroke="var(--primary)" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>`;
    }

    toast.innerHTML = `
        ${iconSvg}
        <span style="font-size:0.9rem; font-weight:500;">${message}</span>
    `;

    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = "0";
        toast.style.transform = "translateY(10px)";
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

window.generateSampleCarrier = function() {
    const canvas = document.createElement("canvas");
    canvas.width = 450;
    canvas.height = 300;
    const ctx = canvas.getContext("2d");

    // Dynamic cyber gradient
    const grad = ctx.createLinearGradient(0, 0, 450, 300);
    grad.addColorStop(0, "#090d16");
    grad.addColorStop(0.5, "#172554");
    grad.addColorStop(1, "#0284c7");
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, 450, 300);

    // Glowing grid lines
    ctx.strokeStyle = "rgba(56, 189, 248, 0.2)";
    ctx.lineWidth = 1;
    for (let x = 0; x < 450; x += 25) {
        ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, 300); ctx.stroke();
    }
    for (let y = 0; y < 300; y += 25) {
        ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(450, y); ctx.stroke();
    }

    // Modern typography badge
    ctx.fillStyle = "#ffffff";
    ctx.font = "bold 24px Inter, sans-serif";
    ctx.fillText("STEGASAFE CARRIER", 80, 140);
    ctx.fillStyle = "#38bdf8";
    ctx.font = "14px monospace";
    ctx.fillText("450 x 300 px | LSB Ready", 120, 170);

    canvas.toBlob((blob) => {
        const file = new File([blob], "stegasafe_test_carrier.png", { type: "image/png" });
        const fileInput = document.getElementById("encodeFileInput");
        // Simulate change event
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(file);
        fileInput.files = dataTransfer.files;
        fileInput.dispatchEvent(new Event("change"));
        showToast("Generated test carrier image (450×300)!", "success");
    }, "image/png");
};

