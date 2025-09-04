# Xconvert - Hadoop-Based File Format Converter

## Overview

**Xconvert** is an advanced file format conversion application that leverages Apache Hadoop's distributed processing capabilities to efficiently convert files between different formats. The application supports both single file conversion and batch processing of multiple files simultaneously.

## Features

- **Multiple Format Support**: Convert between CSV, JSON, XML, and TXT formats.
- **Single File Conversion**: Convert individual files with real-time progress tracking.
- **Batch Processing**: Convert multiple files simultaneously using Hadoop's distributed processing.
- **Drag & Drop Interface**: User-friendly interface for easy file uploads.
- **Automatic Format Detection**: Detects file formats automatically based on the file extension.
- **Progress Tracking**: Provides real-time progress updates during conversion.
- **Consolidated Downloads**: Download all converted files in a single ZIP archive.

## Technology Stack

- **Backend**: Java, Spring Boot
- **Frontend**: HTML, CSS, JavaScript
- **Processing Engine**: Apache Hadoop 3.3.6
- **Build Tool**: Maven 3.8.7
- **Environment**: WSL (Windows Subsystem for Linux) Ubuntu

## System Architecture

Xconvert follows a client-server architecture integrated with Hadoop:

1. **Web Interface**: Developed using HTML, CSS, and JavaScript for user interaction.
2. **REST API**: Built with Spring Boot to handle file uploads and conversion requests.
3. **Hadoop Processing**: Utilizes MapReduce for distributed file processing.
4. **File Storage**: Manages uploaded and converted files efficiently.

## Hadoop Integration

The core functionality of Xconvert is powered by Hadoop’s distributed file processing capabilities:

- **MapReduce Framework**: Uses Hadoop's MapReduce paradigm for file conversion.
- **Parallel Processing**: Handles multiple files concurrently using a thread pool.
- **Scalable Architecture**: Efficiently processes large numbers of files.
- **Fault Tolerance**: Ensures continued operation even if individual file conversions fail.

## Installation and Setup

### Prerequisites

Ensure the following dependencies are installed:

- Java 11 or higher
- Maven 3.8.x
- Hadoop 3.3.x
- WSL Ubuntu or a native Linux environment

### Installation Steps

1. Clone the repository:
   ```sh
   git clone https://github.com/thirusudar03092003/Xconvert-file_converter.git
   cd xconvert
   ```

2. Build the project:
   ```sh
   mvn clean package -DskipTests
   ```

3. Run the application:
   ```sh
   java -jar target/xconvert-1.0-SNAPSHOT.jar
   ```

4. Access the application at:
   ```
   http://localhost:8082
   ```

## Usage

### Single File Conversion

1. Navigate to the **Single File Conversion** tab.
2. Drag & drop a file or click **Browse Files** to select a file.
3. Select the source format (or use auto-detect).
4. Select the target format.
5. Click **Convert**.
6. Download the converted file once processing is complete.

### Batch File Conversion

1. Navigate to the **Batch Conversion** tab.
2. Select the target format for all files.
3. Drag & drop multiple files or click **Browse Files** to select files.
4. Click **Process Files**.
5. Monitor conversion progress in real-time.
6. Download all converted files as a ZIP archive once processing is complete.

## Project Structure

```
Xconvert/
├── pom.xml                     # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/com/xconvert/
│   │   │   ├── App.java        # Spring Boot application entry point
│   │   │   ├── config/         # Configuration classes
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── hadoop/         # Hadoop MapReduce classes
│   │   │   ├── model/          # Data models
│   │   │   ├── service/        # Business logic services
│   │   │   └── util/           # Utility classes
│   │   ├── resources/
│   │   │   ├── application.properties   # Application configuration
│   │   │   ├── static/
│   │   │   │   ├── index.html  # Single file conversion UI
│   │   │   │   ├── batch.html  # Batch processing UI
│   │   │   │   ├── css/        # CSS stylesheets
│   │   │   │   └── js/         # JavaScript files
│   ├── test/                   # Unit test classes
├── uploads/                     # Directory for uploaded files
├── converted/                   # Directory for converted files
└── temp/                        # Directory for temporary files
```

## Hadoop Implementation Details

### MapReduce Components

1. **Mappers**:
   - `CSVToJSONMapper`: Converts CSV lines to JSON objects.
   - Additional mappers handle different conversion types.

2. **Reducers**:
   - `CSVToJSONReducer`: Merges JSON objects into a complete document.
   - Additional reducers handle different conversion types.

3. **Job Configuration**:
   - `ConversionJob`: Manages Hadoop MapReduce jobs.

### Batch Processing Implementation

- Utilizes `ExecutorService` for parallel job execution.
- Uses Spring's `@Async` annotation for asynchronous processing.
- Tracks the status of each file conversion.
- Creates a ZIP file for consolidated downloads.

## Performance

Xconvert is optimized for handling large file batches:

- Successfully tested with **50+ CSV files** in a single batch.
- Processes files in parallel with up to **5 concurrent conversions**.
- Uses memory-efficient streaming for large file handling.
- Designed to scale with Hadoop cluster resources.

## Future Enhancements

Potential improvements include:

1. Support for additional formats (Excel, PDF, etc.).
2. User authentication and conversion history tracking.
3. Custom conversion options (e.g., CSV delimiters).
4. MongoDB integration for file storage.
5. Enhanced error handling and recovery mechanisms.
6. Cloud deployment support.

## Contributing

Contributions are welcome! Feel free to submit a pull request.

## License

This project is licensed under the **MIT License**. See the `LICENSE` file for details.

## Author

Developed by **Thiru Sudar S L** as part of the **B.Tech AI & DS program** at **Velammal Engineering College, Chennai.**.

## Acknowledgments

- **Apache Hadoop** team for their powerful distributed processing framework.
- **Spring Boot** team for their robust web application framework.
- All open-source libraries used in this project.

