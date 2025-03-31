document.addEventListener('DOMContentLoaded', function() {
    const uploadForm = document.getElementById('uploadForm');
    const fileInput = document.getElementById('file');
    const dropArea = document.getElementById('drop-area');
    const fileInfo = document.getElementById('file-info');
    const sourceFormatSelect = document.getElementById('sourceFormat');
    const targetFormatSelect = document.getElementById('targetFormat');
    const progressBar = document.getElementById('progressBar');
    const resultContainer = document.getElementById('resultContainer');
    const resultMessage = document.getElementById('resultMessage');
    const downloadLink = document.getElementById('downloadLink');
    const directDownloadBtn = document.getElementById('directDownloadBtn');
    
    // Base URL for API
    const API_BASE_URL = 'http://localhost:8082/api/convert';
    
    // File format mappings
    const fileExtensionMap = {
        'csv': 'csv',
        'json': 'json',
        'xml': 'xml',
        'txt': 'txt'
    };
    
    // Prevent default drag behaviors
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        dropArea.addEventListener(eventName, preventDefaults, false);
        document.body.addEventListener(eventName, preventDefaults, false);
    });
    
    // Highlight drop area when item is dragged over it
    ['dragenter', 'dragover'].forEach(eventName => {
        dropArea.addEventListener(eventName, highlight, false);
    });
    
    ['dragleave', 'drop'].forEach(eventName => {
        dropArea.addEventListener(eventName, unhighlight, false);
    });
    
    // Handle dropped files
    dropArea.addEventListener('drop', handleDrop, false);
    
    // Handle file selection via input
    fileInput.addEventListener('change', handleFileSelect);
    
    function preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }
    
    function highlight() {
        dropArea.classList.add('highlight');
    }
    
    function unhighlight() {
        dropArea.classList.remove('highlight');
    }
    
    function handleDrop(e) {
        const dt = e.dataTransfer;
        const files = dt.files;
        
        if (files.length > 0) {
            fileInput.files = files;
            handleFileSelect();
        }
    }
    
    function handleFileSelect() {
        if (fileInput.files.length > 0) {
            const file = fileInput.files[0];
            
            // Display file info
            fileInfo.innerHTML = `
                <strong>File:</strong> ${file.name}<br>
                <strong>Size:</strong> ${formatFileSize(file.size)}<br>
                <strong>Type:</strong> ${file.type || 'Unknown'}
            `;
            
            // Auto-detect format
            const fileExtension = getFileExtension(file.name).toLowerCase();
            if (fileExtensionMap[fileExtension]) {
                sourceFormatSelect.value = fileExtensionMap[fileExtension];
            }
        }
    }
    
    function getFileExtension(filename) {
        return filename.split('.').pop();
    }
    
    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
    
    // Poll for task status
    function pollTaskStatus(taskId) {
        return new Promise((resolve, reject) => {
            const checkStatus = () => {
                fetch(`${API_BASE_URL}/status/${taskId}`)
                    .then(response => response.json())
                    .then(data => {
                        console.log('Status check:', data);
                        
                        if (data.status === 'completed') {
                            // Conversion completed
                            resolve(data.outputFileName);
                        } else if (data.status === 'error') {
                            // Conversion failed
                            reject(new Error(data.error || 'Conversion failed'));
                        } else {
                            // Still processing, update progress
                            updateProgress(data.status);
                            // Continue polling
                            setTimeout(checkStatus, 2000);
                        }
                    })
                    .catch(error => {
                        console.error('Error checking status:', error);
                        reject(error);
                    });
            };
            
            // Start polling
            checkStatus();
        });
    }
    
    // Update progress based on status
    function updateProgress(status) {
        if (status === 'processing') {
            // Random progress between 60% and 90% to show activity
            const randomProgress = Math.floor(Math.random() * 30) + 60;
            progressBar.style.width = `${randomProgress}%`;
        }
    }
    
    uploadForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        if (!fileInput.files[0]) {
            alert('Please select a file to convert');
            return;
        }
        
        const sourceFormat = sourceFormatSelect.value;
        const targetFormat = targetFormatSelect.value;
        
        if (!targetFormat) {
            alert('Please select a target format');
            return;
        }
        
        // If source format is auto-detect, get it from the file extension
        let detectedSourceFormat = sourceFormat;
        if (!detectedSourceFormat) {
            const fileExtension = getFileExtension(fileInput.files[0].name).toLowerCase();
            if (fileExtensionMap[fileExtension]) {
                detectedSourceFormat = fileExtensionMap[fileExtension];
            } else {
                alert('Could not detect file format. Please select a source format manually.');
                return;
            }
        }
        
        // Show progress
        progressBar.style.width = '30%';
        resultContainer.style.display = 'block';
        resultMessage.textContent = 'Uploading file...';
        
        // Disable form during processing
        const convertBtn = document.querySelector('#convertBtn');
        convertBtn.disabled = true;
        convertBtn.textContent = 'Processing...';
        
        // Create form data for file upload
        const formData = new FormData();
        formData.append('file', fileInput.files[0]);
        formData.append('sourceFormat', detectedSourceFormat);
        formData.append('targetFormat', targetFormat);
        
        console.log('Uploading file:', fileInput.files[0].name);
        console.log('Source format:', detectedSourceFormat);
        console.log('Target format:', targetFormat);
        
        // Send upload request
        fetch(`${API_BASE_URL}/upload`, {
            method: 'POST',
            body: formData
        })
        .then(response => {
            console.log('Upload response status:', response.status);
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(`File upload failed: ${text}`);
                });
            }
            return response.json();
        })
        .then(data => {
            console.log('Upload successful, task ID:', data.taskId);
            progressBar.style.width = '60%';
            resultMessage.textContent = 'Processing file...';
            
            // Start polling for status
            return pollTaskStatus(data.taskId);
        })
        .then(outputFileName => {
            console.log('Conversion successful, file name:', outputFileName);
            progressBar.style.width = '100%';
            
            // Show result
            resultMessage.textContent = 'File conversion completed successfully!';
            
            // Set download link
            const downloadUrl = `${API_BASE_URL}/download/${outputFileName}`;
            console.log('Download URL:', downloadUrl);
            
            // Update download links
            downloadLink.innerHTML = 'Download Converted File';
            downloadLink.href = downloadUrl;
            downloadLink.download = outputFileName; // Suggest filename to browser
            downloadLink.style.display = 'inline-block';
            
            // Update direct download button
            directDownloadBtn.textContent = 'Direct Download';
            directDownloadBtn.style.display = 'inline-block';
            directDownloadBtn.onclick = function() {
                window.location.href = downloadUrl;
            };
            
            // Re-enable form
            convertBtn.disabled = false;
            convertBtn.textContent = 'Convert';
            
            // Reset progress after a delay
            setTimeout(() => {
                progressBar.style.width = '0%';
            }, 3000);
        })
        .catch(error => {
            console.error('Error:', error);
            progressBar.style.width = '0%';
            resultContainer.style.display = 'block';
            resultMessage.textContent = `Error: ${error.message}`;
            downloadLink.style.display = 'none';
            directDownloadBtn.style.display = 'none';
            
            // Re-enable form
            convertBtn.disabled = false;
            convertBtn.textContent = 'Convert';
        });
    });
});
