let currentPath = "";

function getCurrentPath() {
    currentPath = window.location.pathname.split("/browse/")[1];
}

function showUploadModal() {
    document.getElementById('uploadModal').classList.add('active');
}

function showMkdirModal() {
    document.getElementById('mkdirModal').classList.add('active');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('active');
}

document.addEventListener('DOMContentLoaded', function () {
    const uploadForm = document.getElementById('uploadForm');
    const fileInput = document.getElementById('fileInput');
    const dropZone = document.getElementById('dropZone');
    const fileList = document.getElementById('fileList');

    if (dropZone) {
        dropZone.addEventListener('click', () => fileInput.click());
        dropZone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropZone.classList.add('dragover');
        });
        dropZone.addEventListener('dragleave', () => dropZone.classList.remove('dragover'));
        dropZone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropZone.classList.remove('dragover');
            fileInput.files = e.dataTransfer.files;
            updateFileList();
        });
    }

    if (fileInput) fileInput.addEventListener('change', updateFileList);

    function updateFileList() {
        fileList.innerHTML = '';
        for (const file of fileInput.files) {
            const div = document.createElement('div');
            div.className = 'file-preview-item';
            div.innerHTML = '<span>' + file.name + '</span><span>' + formatSize(file.size) + '</span>';
            fileList.appendChild(div);
        }
    }

    if (uploadForm) {
        uploadForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const files = Array.from(fileInput.files);
            if (files.length === 0) {
                alert('Files to upload may not be null');
                return;
            }

            const formData = new FormData(uploadForm);
            try {
                const res = await fetch('/api/admin/upload', {method: 'POST', body: formData});
                const data = await res.json();
                if (data.success) location.reload();
                else alert(data.message);
            } catch (err) {
                alert('Upload failed: ' + err.message);
            }
        });
    }

    const mkdirForm = document.getElementById('mkdirForm');
    if (mkdirForm) {
        mkdirForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const name = mkdirForm.querySelector('input[name="name"]').value;
            const path = currentPath ? currentPath + '/' + name : name;
            try {
                const res = await fetch('/api/admin/mkdir', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                    body: 'path=' + encodeURIComponent(path)
                });
                const data = await res.json();
                if (data.success) location.reload();
                else alert(data.message);
            } catch (err) {
                alert('Failed: ' + err.message);
            }
        });
    }

    document.querySelectorAll('.modal').forEach(modal => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) closeModal(modal.id);
        });
    });
});

async function deleteItem(path) {
    if (!confirm('Delete ' + path + '?')) return;
    try {
        const res = await fetch('/api/admin/delete', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: 'path=' + encodeURIComponent(path)
        });
        const data = await res.json();
        if (data.success) location.reload();
        else alert(data.message);

    } catch (err) {
        alert('Failed: ' + err.message);
    }
}

async function renameItem(path) {
    const name = path.split('/').pop();
    const newName = prompt('New name:', name);
    if (!newName || newName === name) return;
    const dir = path.substring(0, path.length - name.length);
    const newPath = dir + newName;
    try {
        const res = await fetch('/api/admin/move', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: 'from=' + encodeURIComponent(path) + '&to=' + encodeURIComponent(newPath)
        });
        const data = await res.json();
        if (data.success) location.reload();
        else alert(data.message);
    } catch (err) {
        alert('Failed: ' + err.message);
    }
}

function formatSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KiB';
    if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MiB';
    return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GiB';
}

window.onload = getCurrentPath;