# Xconvert - Hadoop-Based File Format Converter

## Overview

Xconvert is an advanced file format converter application that leverages Apache Hadoop's distributed processing capabilities to efficiently convert files between different formats. The application supports both single file conversion and batch processing of multiple files simultaneously.

![Xconvert Logo](https://via.placeholder.com/150/3498db/FFFFFF?text=Xconvert)

## Features

- **Multiple Format Support**: Convert between CSV, JSON, XML, and TXT formats
- **Single File Conversion**: Convert individual files with real-time progress tracking
- **Batch Processing**: Convert multiple files simultaneously using Hadoop's distributed processing
- **Drag & Drop Interface**: User-friendly interface for file uploads
- **Automatic Format Detection**: Automatically detects file formats based on extension
- **Progress Tracking**: Real-time progress updates during conversion
- **Consolidated Downloads**: Download all converted files in a single ZIP archive

## Technology Stack

- **Backend**: Java, Spring Boot
- **Frontend**: HTML, CSS, JavaScript
- **Processing Engine**: Apache Hadoop 3.3.6
- **Build Tool**: Maven 3.8.7
- **Environment**: WSL (Windows Subsystem for Linux) Ubuntu

## System Architecture

Xconvert follows a client-server architecture with Hadoop integration:

1. **Web Interface**: HTML/CSS/JS frontend for user interaction
2. **REST API**: Spring Boot backend for handling file uploads and conversion requests
3. **Hadoop Processing**: MapReduce jobs for distributed file processing
4. **File Storage**: File system for storing uploaded and converted files

## Hadoop Integration

The core of Xconvert is its Hadoop integration for distributed file processing:

- **MapReduce Framework**: Uses Hadoop's MapReduce paradigm for file conversion
- **Parallel Processing**: Processes multiple files concurrently using a thread pool
- **Scalable Architecture**: Designed to handle large numbers of files efficiently
- **Fault Tolerance**: Continues processing even if individual file conversions fail

## Installation and Setup

### Prerequisites

- Java 11 or higher
- Maven 3.8.x
- Hadoop 3.3.x
- WSL Ubuntu or native Linux environment

### Installation Steps

1. Clone the repository:
git clone https://github.com/yourusername/xconvert.git
cd xconvert

plaintext
Type into WindowsTerminal.exe
Copy

2. Build the project:
mvn clean package -DskipTests

plaintext
Type into WindowsTerminal.exe
Copy

3. Run the application:
java -jar target/xconvert-1.0-SNAPSHOT.jar

plaintext
Type into WindowsTerminal.exe
Copy

4. Access the application:
http://localhost:8082

plaintext
Type into WindowsTerminal.exe
Copy

## Usage

### Single File Conversion

1. Navigate to the "Single File Conversion" tab
2. Drag & drop a file or click "Browse Files" to select a file
3. Select the source format (or use auto-detect)
4. Select the target format
5. Click "Convert"
6. Download the converted file when processing is complete

### Batch File Conversion

1. Navigate to the "Batch Conversion" tab
2. Select the target format for all files
3. Drag & drop multiple files or click "Browse Files" to select files
4. Click "Process Files"
5. Monitor conversion progress in real-time
6. Download all converted files as a ZIP archive when processing is complete

## Project Structure

Xconvert/
├── xconvert/
│   ├── pom.xml                                  # Maven configuration
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── xconvert/
│   │   │   │           ├── App.java             # Spring Boot application
│   │   │   │           ├── config/              # Configuration classes
│   │   │   │           ├── controller/          # REST controllers
│   │   │   │           ├── hadoop/              # Hadoop MapReduce classes
│   │   │   │           ├── model/               # Data models
│   │   │   │           ├── service/             # Business logic services
│   │   │   │           └── util/                # Utility classes
│   │   │   └── resources/
│   │   │       ├── application.properties       # Application configuration
│   │   │       └── static/                      # Frontend files
│   │   │           ├── index.html              # Single file conversion UI
│   │   │           ├── batch.html              # Batch processing UI
│   │   │           ├── css/                    # CSS stylesheets
│   │   │           └── js/                     # JavaScript files
│   │   └── test/
│   │       └── java/                            # Test classes
├── uploads/                                     # Directory for uploaded files
├── converted/                                   # Directory for converted files
└── temp/                                        # Directory for temporary files

plaintext
Type into WindowsTerminal.exe
Copy

## Hadoop Implementation Details

### MapReduce Classes

1. **Mappers**:
   - `CSVToJSONMapper`: Converts CSV lines to JSON objects
   - Other mappers for different conversion types

2. **Reducers**:
   - `CSVToJSONReducer`: Combines JSON objects into a complete document
   - Other reducers for different conversion types

3. **Job Configuration**:
   - `ConversionJob`: Sets up and manages Hadoop MapReduce jobs

### Batch Processing

The batch processing implementation uses:

1. `ExecutorService` for parallel job execution
2. Asynchronous processing with Spring's `@Async` annotation
3. Status tracking for each file conversion
4. ZIP file creation for consolidated downloads

## Performance

Xconvert is designed to handle large volumes of files efficiently:

- Successfully tested with 50+ CSV files in a single batch
- Processes files in parallel (up to 5 concurrent conversions)
- Memory-efficient streaming for large file processing
- Scalable with Hadoop cluster resources

## Future Enhancements

Potential future improvements include:

1. Additional file format support (Excel, PDF, etc.)
2. User authentication and conversion history
3. Custom conversion options (e.g., CSV delimiters)
4. MongoDB integration for file storage
5. Advanced error handling and recovery
6. Cloud deployment support

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Author

Developed by Thiru Sudar S L as part of B.Tech AI & DS program at Velammal Engineering College, Ambattur.

## Acknowledgments

- Apache Hadoop team for the powerful distributed processing framework
- Spring Boot team for the excellent web application framework
- All open-source libraries used in this project
