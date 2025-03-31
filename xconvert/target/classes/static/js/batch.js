document.addEventListener('DOMContentLoaded', function() {
    // Elements
    const step1 = document.getElementById('step1');
    const step2 = document.getElementById('step2');
    const step3 = document.getElementById('step3');
    
    const targetFormatSelect = document.getElementById('targetFormat');
    const createBatchBtn = document.getElementById('createBatchBtn');
    const batchDropArea = document.getElementById('batchDropArea');
    const batchFiles = document.getElementById('batchFiles');
    const fileList = document.getElementById('fileList');
    const backToStep1Btn = document.getElementById('backToStep1Btn');
    const processFilesBtn = document.getElementById('processFilesBtn');
    const completedCount = document.getElementById('completedCount');
    const totalCount = document.getElementById('totalCount');
    const batchProgressBar = document.getElementById('batchProgressBar');
    const processingStatus = document.getElementById('processingStatus');
    const downloadBatchBtn = document.getElementById('downloadBatchBtn');
    
    // API Base URL
    const API_BASE_URL = 'http://localhost:8082/api/batch';
    
    // Batch job data
    let batchJobId = null;
    let uploadedFiles = [];
    let pollingInterval = null;
    
    // File format mappings
    const fileExtensionMap = {
        'csv': 'csv',
        'json': 'json',
        'xml': 'xml',
        'txt': 'txt'
    };
    
    // Event listeners
    createBatchBtn.addEventListener('click', createBatchJob);
    backToStep1Btn.addEventListener('click', goToStep1);
    processFilesBtn.addEventListener('click', processFiles);
    downloadBatchBtn.addEventListener('click', downloadBatchResults);
    
    // Drag and drop functionality
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        batchDropArea.addEventListener(eventName, preventDefaults, false);
    });
    
    ['dragenter', 'dragover'].forEach(eventName => {
        batchDropArea.addEventListener(eventName, highlight, false);
    });
    
    ['dragleave', 'drop'].forEach(eventName => {
        batchDropArea.addEventListener(eventName, unhighlight, false);
    });
    
    batchDropArea.addEventListener('drop', handleDrop, false);
    batchFiles.addEventListener('change', handleFileSelect);
    
    function preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }
    
    function highlight() {
        batchDropArea.classList.add('highlight');
    }
    
    function unhighlight() {
        batchDropArea.classList.remove('highlight');
    }
    
    function handleDrop(e) {
        const dt = e.dataTransfer;
        const files = dt.files;
        
        if (files.length > 0) {
            handleFiles(files);
        }
    }
    
    function handleFileSelect(e) {
        const files = e.target.files;
        if (files.length > 0) {
            handleFiles(files);
        }
    }
    
    function handleFiles(files) {
        for (let i = 0; i < files.length; i++) {
            uploadFile(files[i]);
        }
    }
    
    function createBatchJob() {
        const targetFormat = targetFormatSelect.value;
        
        if (!targetFormat) {
            alert('Please select a target format');
            return;
        }
        
        // Create batch job
        fetch(`${API_BASE_URL}/create?targetFormat=${targetFormat}`, {
            method: 'POST'
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to create batch job');
            }
            return response.json();
        })
        .then(data => {
            batchJobId = data.batchJobId;
            console.log('Batch job created:', batchJobId);
            
            // Clear previous files
            uploadedFiles = [];
            fileList.innerHTML = '';
            
            // Go to step 2
            goToStep2();
        })
        .catch(error => {
            console.error('Error creating batch job:', error);
            alert(`Error creating batch job: ${error.message}`);
        });
    }
    
    function uploadFile(file) {
        if (!batchJobId) {
            alert('Please create a batch job first');
            return;
        }
        
        // Add file to list with pending status
        const fileId = Date.now().toString();
        const fileItem = createFileItem(file, fileId, 'pending');
        fileList.appendChild(fileItem);
        
        // Create form data
        const formData = new FormData();
        formData.append('file', file);
        
        // Upload file
        fetch(`${API_BASE_URL}/upload/${batchJobId}`, {
            method: 'POST',
            body: formData
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to upload file');
            }
            return response.json();
        })
        .then(data => {
            console.log('File uploaded:', data);
            
            // Update file item with server-assigned ID
            const existingItem = document.getElementById(`file-${fileId}`);
            if (existingItem) {
                existingItem.id = `file-${data.itemId}`;
                
                // Add to uploaded files
                uploadedFiles.push({
                    id: data.itemId,
                    name: file.name,
                    size: file.size
                });
            }
        })
        .catch(error => {
            console.error('Error uploading file:', error);
            
            // Update file item to show error
            const existingItem = document.getElementById(`file-${fileId}`);
            if (existingItem) {
                const statusElement = existingItem.querySelector('.file-status');
                statusElement.textContent = 'Failed';
                statusElement.className = 'file-status status-failed';
            }
        });
    }
    
    function createFileItem(file, id, status) {
        const fileItem = document.createElement('div');
        fileItem.className = 'file-item';
        fileItem.id = `file-${id}`;
        
        const fileIcon = document.createElement('div');
        fileIcon.className = 'file-icon';
        fileIcon.innerHTML = '<i class="fas fa-file"></i>'; // Requires Font Awesome
        
        const fileName = document.createElement('div');
        fileName.className = 'file-name';
        fileName.textContent = file.name;
        
        const fileSize = document.createElement('div');
        fileSize.className = 'file-size';
        fileSize.textContent = formatFileSize(file.size);
        
        const fileStatus = document.createElement('div');
        fileStatus.className = `file-status status-${status}`;
        fileStatus.textContent = status.charAt(0).toUpperCase() + status.slice(1);
        
        fileItem.appendChild(fileIcon);
        fileItem.appendChild(fileName);
        fileItem.appendChild(fileSize);
        fileItem.appendChild(fileStatus);
        
        return fileItem;
    }
    
    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
    
    function processFiles() {
        if (!batchJobId) {
            alert('Please create a batch job first');
            return;
        }
        
        if (uploadedFiles.length === 0) {
            alert('Please upload at least one file');
            return;
        }
        
        // Start processing
        fetch(`${API_BASE_URL}/process/${batchJobId}`, {
            method: 'POST'
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to start processing');
            }
            return response.json();
        })
        .then(data => {
            console.log('Processing started:', data);
            
            // Go to step 3
            goToStep3();
            
            // Start polling for status
            startPolling();
        })
        .catch(error => {
            console.error('Error starting processing:', error);
            alert(`Error starting processing: ${error.message}`);
        });
    }
    
    function startPolling() {
        // Update status immediately
        updateBatchStatus();
        
        // Set up polling interval
        pollingInterval = setInterval(updateBatchStatus, 2000);
    }
    
    function stopPolling() {
        if (pollingInterval) {
            clearInterval(pollingInterval);
            pollingInterval = null;
        }
    }
    
    function updateBatchStatus() {
        fetch(`${API_BASE_URL}/status/${batchJobId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to get batch status');
                }
                return response.json();
            })
            .then(data => {
                console.log('Batch status:', data);
                
                // Update progress
                const total = data.totalItems;
                const completed = data.completedItems + data.failedItems;
                const percentage = total > 0 ? (completed / total) * 100 : 0;
                
                totalCount.textContent = total;
                completedCount.textContent = completed;
                batchProgressBar.style.width = `${percentage}%`;
                
                // Update status of each file
                updateFileStatuses(data.items);
                
                // Check if processing is complete
                if (data.status === 'completed' || data.status === 'completed_with_errors' || data.status === 'failed') {
                    stopPolling();
                    
                    // Enable download button if there are completed items
                    if (data.completedItems > 0) {
                        downloadBatchBtn.disabled = false;
                    }
                    
                    // Add summary
                    const summary = document.createElement('div');
                    summary.className = 'status-item ' + (data.status === 'failed' ? 'error' : 'success');
                    summary.textContent = `Batch processing ${data.status}: ${data.completedItems} of ${total} files converted successfully.`;
                    processingStatus.appendChild(summary);
                }
            })
            .catch(error => {
                console.error('Error getting batch status:', error);
                stopPolling();
            });
    }
    
    function updateFileStatuses(items) {
        if (!items) return;
        
        items.forEach(item => {
            const fileItem = document.getElementById(`file-${item.id}`);
            if (fileItem) {
                const statusElement = fileItem.querySelector('.file-status');
                statusElement.textContent = item.status.charAt(0).toUpperCase() + item.status.slice(1);
                statusElement.className = `file-status status-${item.status}`;
                
                // Add status message to processing status
                if (item.status === 'completed' || item.status === 'failed') {
                    // Check if status is already added
                    const existingStatus = document.getElementById(`status-${item.id}`);
                    if (!existingStatus) {
                        const statusItem = document.createElement('div');
                        statusItem.id = `status-${item.id}`;
                        statusItem.className = `status-item ${item.status === 'completed' ? 'success' : 'error'}`;
                        
                        const fileName = document.createElement('span');
                        fileName.textContent = item.originalFilename;
                        
                        const statusText = document.createElement('span');
                        statusText.textContent = item.status === 'completed' ? 'Converted successfully' : `Failed: ${item.errorMessage}`;
                        
                        statusItem.appendChild(fileName);
                        statusItem.appendChild(statusText);
                        
                        processingStatus.appendChild(statusItem);
                    }
                }
            }
        });
    }
    
    function downloadBatchResults() {
        if (!batchJobId) {
            alert('No batch job to download');
            return;
        }
        
        window.location.href = `${API_BASE_URL}/download/${batchJobId}`;
    }
    
    function goToStep1() {
        step1.classList.add('active');
        step2.classList.remove('active');
        step3.classList.remove('active');
        
        // Stop polling if active
        stopPolling();
    }
    
    function goToStep2() {
        step1.classList.remove('active');
        step2.classList.add('active');
        step3.classList.remove('active');
        
        // Stop polling if active
        stopPolling();
    }
    
    function goToStep3() {
        step1.classList.remove('active');
        step2.classList.remove('active');
        step3.classList.add('active');
        
        // Reset progress display
        completedCount.textContent = '0';
        totalCount.textContent = uploadedFiles.length.toString();
        batchProgressBar.style.width = '0%';
        processingStatus.innerHTML = '';
        downloadBatchBtn.disabled = true;
    }
});
