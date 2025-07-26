package com.b07group32.relationsafe;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Map;
import java.util.UUID;

class DocumentUploadManager {
    private static final String TAG = "DocumentUploadManager";
    private static final long MAX_FILE_SIZE = 30 * 1024 * 1024; // 30MB limit per doc

    private Fragment fragment;
    private FirebaseUser currentUser;
    private DatabaseReference databaseRef; // Realtime DB - stores ALL metadata & file references
    private FirebaseStorage storage;       // Storage - stores JUST the raw blob data
    private StorageReference storageRef;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Uri selectedFileUri;
    private String selectedFileName;
    private Button currentUploadButton;

    // Callback interface for file selection
    public interface FileSelectionCallback {
        void onFileSelected(String fileName);
        void onFileSelectionFailed(String error);
    }

    // Callback interface for upload completion
    public interface UploadCallback {
        void onUploadSuccess(String message);
        void onUploadFailure(String error);
    }

    private FileSelectionCallback fileSelectionCallback;
    private UploadCallback uploadCallback;

    public DocumentUploadManager(Fragment fragment, FirebaseUser currentUser, DatabaseReference databaseRef)
    {
        this.fragment = fragment;
        this.currentUser = currentUser;
        this.databaseRef = databaseRef;
        this.storage = FirebaseStorage.getInstance();
        this.storageRef = storage.getReference();

        initializeFilePicker();
    }

    private void initializeFilePicker()
    {
        filePickerLauncher = fragment.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result ->
                {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null)
                    {
                        selectedFileUri = result.getData().getData();

                        if (selectedFileUri != null)
                        {
                            selectedFileName = FileUtils.getFileName(fragment.getContext(), selectedFileUri);

                            // check that the file is small enough so I don't get slammed with a massive bill
                            if (isFileSizeValid(selectedFileUri))
                            {
                                if (currentUploadButton != null)
                                {
                                    currentUploadButton.setText("Selected: " + selectedFileName);
                                }

                                // Notify callback
                                if (fileSelectionCallback != null) {
                                    fileSelectionCallback.onFileSelected(selectedFileName);
                                }

                                Log.d(TAG, "File selected: " + selectedFileName);
                            }
                            else
                            {
                                selectedFileUri = null;
                                selectedFileName = null;
                                String error = "File is too large. Maximum size is 30MB.";
                                Toast.makeText(fragment.getContext(), error, Toast.LENGTH_LONG).show();

                                if (fileSelectionCallback != null) {
                                    fileSelectionCallback.onFileSelectionFailed(error);
                                }
                            }
                        }
                    }
                    else
                    {
                        Log.w(TAG, "File selection cancelled or failed");
                        if (fileSelectionCallback != null) {
                            fileSelectionCallback.onFileSelectionFailed("File selection cancelled");
                        }
                    }
                }
        );
    }

    private boolean isFileSizeValid(Uri fileUri)
    {
        try
        {
            long fileSize = FileUtils.getFileSize(fragment.getContext(), fileUri);
            return fileSize <= MAX_FILE_SIZE;
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error checking file size", e);
            return false;
        }
    }

    public void setupUploadButton(Button button, String[] mimeTypes)
    {
        currentUploadButton = button;
        selectedFileUri = null;
        selectedFileName = null;
        button.setText("Select Document");
        button.setOnClickListener(v -> openFilePicker(mimeTypes));
    }

    // New method for file selection without button binding
    public void selectFile(String[] mimeTypes, FileSelectionCallback callback) {
        this.fileSelectionCallback = callback;
        openFilePicker(mimeTypes);
    }

    private void openFilePicker(String[] mimeTypes)
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        if (mimeTypes != null && mimeTypes.length > 0)
        {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try
        {
            filePickerLauncher.launch(Intent.createChooser(intent, "Select Document"));
            Log.d(TAG, "File picker launched");
        }
        catch (android.content.ActivityNotFoundException ex)
        {
            Log.e(TAG, "No file manager available", ex);
            String error = "Please install a file manager";
            Toast.makeText(fragment.getContext(), error, Toast.LENGTH_SHORT).show();
            if (fileSelectionCallback != null) {
                fileSelectionCallback.onFileSelectionFailed(error);
            }
        }
    }

    public void uploadAndSave(String category, Map<String, String> formData)
    {
        uploadAndSave(category, formData, null);
    }

    public void uploadAndSave(String category, Map<String, String> formData, UploadCallback callback) {
        this.uploadCallback = callback;
        Log.d(TAG, "uploadAndSave called for category: " + category);

        if (selectedFileUri != null) {
            Log.d(TAG, "Uploading blob to Storage, metadata to Realtime DB: " + selectedFileName);
            uploadFile(category, formData, selectedFileUri);
        } else {
            Log.d(TAG, "No file selected, saving form data only to Realtime DB");
            saveMetadata(category, formData, null, null);
        }
    }

    private void uploadFileForUpdate(DatabaseReference itemRef, Map<String, String> formData, Uri fileUri, String oldBlobReference) {
        if (currentUser == null) {
            String error = "Authentication error";
            Log.e(TAG, error);
            Toast.makeText(fragment.getContext(), error, Toast.LENGTH_SHORT).show();
            if (uploadCallback != null) {
                uploadCallback.onUploadFailure(error);
            }
            return;
        }

        Toast.makeText(fragment.getContext(), "Uploading new file...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Starting blob upload to Storage for update");

        try {
            String fileExtension = FileUtils.getFileExtension(fragment.getContext(), fileUri);
            String blobId = UUID.randomUUID().toString();
            String storageBlobName = blobId + "." + fileExtension;

            StorageReference blobRef = storageRef
                    .child("blobs")
                    .child(currentUser.getUid()).child(storageBlobName);

            UploadTask uploadTask = blobRef.putFile(fileUri);

            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG, "Blob upload progress: " + progress + "%");
            }).addOnSuccessListener(taskSnapshot -> {
                Log.d(TAG, "Blob upload successful to Storage");

                String blobReference = blobRef.getPath();

                // Delete old blob if it exists
                if (oldBlobReference != null && !oldBlobReference.isEmpty()) {
                    StorageReference oldBlobRef = storage.getReference().child(oldBlobReference);
                    oldBlobRef.delete().addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Old blob deleted successfully");
                    }).addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to delete old blob: " + e.getMessage());
                    });
                }

                // Update item with new blob information
                updateItemWithBlobRef(itemRef, formData, blobReference, selectedFileName, blobId);

            }).addOnFailureListener(e -> {
                Log.e(TAG, "Blob upload failed", e);
                String errorMessage = "Upload failed: " + e.getMessage();

                if (e.getMessage() != null) {
                    if (e.getMessage().contains("permission")) {
                        errorMessage = "Permission denied. Check Firebase Storage rules.";
                    } else if (e.getMessage().contains("network")) {
                        errorMessage = "Network error. Check your internet connection.";
                    } else if (e.getMessage().contains("quota")) {
                        errorMessage = "Files are too big.";
                    }
                }

                Toast.makeText(fragment.getContext(), errorMessage, Toast.LENGTH_LONG).show();
                if (uploadCallback != null) {
                    uploadCallback.onUploadFailure(errorMessage);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Exception during blob upload setup", e);
            String error = "Error preparing upload: " + e.getMessage();
            Toast.makeText(fragment.getContext(), error, Toast.LENGTH_LONG).show();
            if (uploadCallback != null) {
                uploadCallback.onUploadFailure(error);
            }
        }
    }

    private void updateItemWithBlobRef(DatabaseReference itemRef, Map<String, String> formData, String blobReference, String originalFileName, String blobId) {
        Log.d(TAG, "Updating item with new file metadata");

        formData.put("timestamp", String.valueOf(System.currentTimeMillis()));
        formData.put("blobReference", blobReference);
        formData.put("originalFileName", originalFileName);
        formData.put("blobId", blobId);
        formData.put("fileSize", String.valueOf(getSelectedFileSize()));
        formData.put("mimeType", FileUtils.getMimeType(fragment.getContext(), selectedFileUri));

        itemRef.updateChildren((Map) formData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Item updated successfully with new file");
                    String success = "Item updated successfully with new file";
                    Toast.makeText(fragment.getContext(), success, Toast.LENGTH_SHORT).show();
                    if (uploadCallback != null) {
                        uploadCallback.onUploadSuccess(success);
                    }
                    clearSelection();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update item with file metadata", e);
                    String error = "Failed to update: " + e.getMessage();
                    Toast.makeText(fragment.getContext(), error, Toast.LENGTH_LONG).show();
                    if (uploadCallback != null) {
                        uploadCallback.onUploadFailure(error);
                    }
                });
    }

    private void updateItemMetadata(DatabaseReference itemRef, Map<String, String> formData) {
        Log.d(TAG, "Updating item metadata only (no file change)");

        formData.put("timestamp", String.valueOf(System.currentTimeMillis()));

        itemRef.updateChildren((Map) formData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Item metadata updated successfully");
                    String success = "Item updated successfully";
                    Toast.makeText(fragment.getContext(), success, Toast.LENGTH_SHORT).show();
                    if (uploadCallback != null) {
                        uploadCallback.onUploadSuccess(success);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update item metadata", e);
                    String error = "Failed to update: " + e.getMessage();
                    Toast.makeText(fragment.getContext(), error, Toast.LENGTH_LONG).show();
                    if (uploadCallback != null) {
                        uploadCallback.onUploadFailure(error);
                    }
                });
    }

    private void uploadFile(String category, Map<String, String> formData, Uri fileUri)
    {
        if (currentUser == null)
        {
            String error = "User not authenticated";
            Log.e(TAG, error);
            Toast.makeText(fragment.getContext(), "Authentication error", Toast.LENGTH_SHORT).show();
            if (uploadCallback != null) {
                uploadCallback.onUploadFailure(error);
            }
            return;
        }

        Toast.makeText(fragment.getContext(), "Uploading blob...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Starting blob upload to Storage for user: " + currentUser.getUid());

        try
        {
            String fileExtension = FileUtils.getFileExtension(fragment.getContext(), fileUri);
            String blobId = UUID.randomUUID().toString(); // blob identifier
            String storageBlobName = blobId + "." + fileExtension;

            // Firebase Storage - just for storing the raw blob data
            StorageReference blobRef = storageRef
                    .child("blobs")
                    .child(currentUser.getUid()).child(storageBlobName);

            Log.d(TAG, "Blob storage path: " + blobRef.getPath());

            UploadTask uploadTask = blobRef.putFile(fileUri);

            uploadTask.addOnProgressListener(taskSnapshot ->
            {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG, "Blob upload progress: " + progress + "%");
            }).addOnSuccessListener(taskSnapshot ->
            {
                Log.d(TAG, "Blob upload successful to Storage");

                // Get the blob reference (not download URL for security)
                String blobReference = blobRef.getPath(); // This is just the storage path

                Log.d(TAG, "Blob reference: " + blobReference);

                // Store ALL file information in Realtime Database
                saveMetadataWithBlobRef(category, formData, blobReference, selectedFileName, blobId);

                String success = "File uploaded successfully!";
                Toast.makeText(fragment.getContext(), success, Toast.LENGTH_SHORT).show();
                if (uploadCallback != null) {
                    uploadCallback.onUploadSuccess(success);
                }

                // Reset the button
                if (currentUploadButton != null)
                {
                    currentUploadButton.setText("Select Document");
                }
                selectedFileUri = null;
                selectedFileName = null;

            }).addOnFailureListener(e ->
            {
                Log.e(TAG, "Blob upload failed", e);
                String errorMessage = "Upload failed: " + e.getMessage();

                // Provide more specific error messages
                if (e.getMessage() != null) {
                    if (e.getMessage().contains("permission"))
                    {
                        errorMessage = "Permission denied. Check Firebase Storage rules.";
                    }
                    else if (e.getMessage().contains("network"))
                    {
                        errorMessage = "Network error. Check your internet connection.";
                    }
                    else if (e.getMessage().contains("quota"))
                    {
                        errorMessage = "Files are too big.";
                    }
                }

                Toast.makeText(fragment.getContext(), errorMessage, Toast.LENGTH_LONG).show();
                if (uploadCallback != null) {
                    uploadCallback.onUploadFailure(errorMessage);
                }
            });

        }
        catch (Exception e)
        {
            Log.e(TAG, "Exception during blob upload setup", e);
            String error = "Error preparing upload: " + e.getMessage();
            Toast.makeText(fragment.getContext(), error, Toast.LENGTH_LONG).show();
            if (uploadCallback != null) {
                uploadCallback.onUploadFailure(error);
            }
        }
    }

    //Delete this function and the above if 'metadata' turns out to only be descriptions
    private void saveMetadataWithBlobRef(String category, Map<String, String> formData, String blobReference, String originalFileName, String blobId)
    {
        if (currentUser == null)
        {
            Log.e(TAG, "User not authenticated for metadata save");
            if (uploadCallback != null) {
                uploadCallback.onUploadFailure("User not authenticated");
            }
            return;
        }

        Log.d(TAG, "Saving file metadata to Realtime Database for category: " + category);

        // Realtime Database stores ALL the file information
        formData.put("timestamp", String.valueOf(System.currentTimeMillis()));
        formData.put("blobReference", blobReference);    // Reference to blob in Storage
        formData.put("originalFileName", originalFileName); // User-friendly filename
        formData.put("blobId", blobId);                   // Unique blob identifier
        formData.put("fileSize", String.valueOf(getSelectedFileSize())); // File size
        formData.put("mimeType", FileUtils.getMimeType(fragment.getContext(), selectedFileUri)); // MIME type

        Log.d(TAG, "File metadata - Name: " + originalFileName + ", Blob: " + blobReference);

        String itemId = databaseRef.child(category).push().getKey();
        if (itemId != null)
        {
            Log.d(TAG, "Saving to Realtime Database with ID: " + itemId);

            databaseRef.child(category).child(itemId).setValue(formData)
                    .addOnSuccessListener(aVoid ->
                    {
                        Log.d(TAG, "File metadata saved successfully to Realtime Database");
                        String success = "Item with file added successfully";
                        Toast.makeText(fragment.getContext(), success, Toast.LENGTH_SHORT).show();
                        if (uploadCallback != null) {
                            uploadCallback.onUploadSuccess(success);
                        }
                    })
                    .addOnFailureListener(e ->
                    {
                        Log.e(TAG, "Failed to save file metadata to Realtime Database", e);
                        String error = "Failed to save: " + e.getMessage();
                        Toast.makeText(fragment.getContext(), error, Toast.LENGTH_LONG).show();
                        if (uploadCallback != null) {
                            uploadCallback.onUploadFailure(error);
                        }
                    });
        }
        else
        {
            Log.e(TAG, "Failed to generate database key");
            String error = "Database error: Could not generate key";
            Toast.makeText(fragment.getContext(), error, Toast.LENGTH_SHORT).show();
            if (uploadCallback != null) {
                uploadCallback.onUploadFailure(error);
            }
        }
    }

    private void saveMetadata(String category, Map<String, String> formData, String downloadUrl, String fileName)
    {
        if (currentUser == null)
        {
            Log.e(TAG, "User not authenticated for metadata save");
            if (uploadCallback != null) {
                uploadCallback.onUploadFailure("User not authenticated");
            }
            return;
        }

        Log.d(TAG, "Saving non-file metadata to Realtime Database for category: " + category);

        // For items (people, locations) without files, just store the form data
        // Maybe we add a picture for the people, like for their faces or ID
        formData.put("timestamp", String.valueOf(System.currentTimeMillis()));

        String itemId = databaseRef.child(category).push().getKey();
        if (itemId != null)
        {
            Log.d(TAG, "Saving to Realtime Database with ID: " + itemId);

            databaseRef.child(category).child(itemId).setValue(formData)
                    .addOnSuccessListener(aVoid ->
                    {
                        Log.d(TAG, "Metadata saved successfully to Realtime Database");
                        String success = "Item added successfully";
                        Toast.makeText(fragment.getContext(), success, Toast.LENGTH_SHORT).show();
                        if (uploadCallback != null) {
                            uploadCallback.onUploadSuccess(success);
                        }
                    })
                    .addOnFailureListener(e ->
                    {
                        Log.e(TAG, "Failed to save metadata to Realtime Database", e);
                        String error = "Failed to save: " + e.getMessage();
                        Toast.makeText(fragment.getContext(), error, Toast.LENGTH_LONG).show();
                        if (uploadCallback != null) {
                            uploadCallback.onUploadFailure(error);
                        }
                    });
        }
        else
        {
            Log.e(TAG, "Failed to generate database key");
            String error = "Database error: Could not generate key";
            Toast.makeText(fragment.getContext(), error, Toast.LENGTH_SHORT).show();
            if (uploadCallback != null) {
                uploadCallback.onUploadFailure(error);
            }
        }
    }

    // Method to check if a file is currently selected
    // use later for metadata and be more robust
    public boolean hasSelectedFile() {
        return selectedFileUri != null;
    }

    // Method to get selected file size
    private long getSelectedFileSize()
    {
        if (selectedFileUri != null)
        {
            return FileUtils.getFileSize(fragment.getContext(), selectedFileUri);
        }
        return 0;
    }

    // Method to get download URL from blob reference
    public void getDownloadUrl(String blobReference, DownloadUrlCallback callback) {
        if (blobReference == null || blobReference.isEmpty()) {
            callback.onFailure("Invalid blob reference");
            return;
        }

        StorageReference blobRef = storage.getReference().child(blobReference);
        blobRef.getDownloadUrl()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }


    public void setupUpdateButton(Button button, String[] mimeTypes, String currentFileName) {
        currentUploadButton = button;
        selectedFileUri = null;
        selectedFileName = null;

        if (currentFileName != null && !currentFileName.isEmpty()) {
            button.setText("Current: " + currentFileName + " (Tap to change)");
        } else {
            button.setText("Select Document");
        }

        button.setOnClickListener(v -> openFilePicker(mimeTypes));
    }

    // Method to check if user has selected a new file for update
    public boolean hasNewFileSelected() {
        return selectedFileUri != null;
    }

    // Method to get current selection info for display
    public String getSelectionInfo() {
        if (selectedFileUri != null && selectedFileName != null) {
            long fileSize = getSelectedFileSize();
            String sizeText = formatFileSize(fileSize);
            return "New file selected: " + selectedFileName + " (" + sizeText + ")";
        }
        return null;
    }

    // Helper method to format file size
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }

    // Enhanced method to handle file updates with better error handling
    public void updateExistingItem(DatabaseReference itemRef, Map<String, String> formData,
                                   String oldBlobReference, UploadCallback callback) {
        this.uploadCallback = callback;

        Log.d(TAG, "Updating item - Has new file: " + (selectedFileUri != null));

        if (selectedFileUri != null) {
            // Upload new file and update the item
            uploadFileForUpdate(itemRef, formData, selectedFileUri, oldBlobReference);
        } else {
            // Just update the text fields
            updateItemMetadata(itemRef, formData);
        }
    }

    // Method to get file info from blob reference (useful for previews)
    public void getFileInfo(String blobReference, FileInfoCallback callback) {
        if (blobReference == null || blobReference.isEmpty()) {
            callback.onFailure("Invalid blob reference");
            return;
        }

        StorageReference blobRef = storage.getReference().child(blobReference);

        blobRef.getMetadata()
                .addOnSuccessListener(storageMetadata -> {
                    String name = storageMetadata.getName();
                    long size = storageMetadata.getSizeBytes();
                    String contentType = storageMetadata.getContentType();

                    callback.onSuccess(name, size, contentType);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
    public interface DownloadUrlCallback {
        void onSuccess(Uri downloadUrl);
        void onFailure(String error);
    }
    public interface FileInfoCallback {
        void onSuccess(String fileName, long fileSize, String mimeType);
        void onFailure(String error);
    }

    // Method to validate file before upload (can be called from UI)
    public boolean validateSelectedFile() {
        if (selectedFileUri == null) {
            return true; // No file selected is valid for updates
        }

        try {
            long fileSize = FileUtils.getFileSize(fragment.getContext(), selectedFileUri);
            if (fileSize > MAX_FILE_SIZE) {
                String error = "File is too large. Maximum size is " + formatFileSize(MAX_FILE_SIZE) + ".";
                Toast.makeText(fragment.getContext(), error, Toast.LENGTH_LONG).show();
                if (fileSelectionCallback != null) {
                    fileSelectionCallback.onFileSelectionFailed(error);
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error validating file", e);
            String error = "Error checking file: " + e.getMessage();
            Toast.makeText(fragment.getContext(), error, Toast.LENGTH_SHORT).show();
            if (fileSelectionCallback != null) {
                fileSelectionCallback.onFileSelectionFailed(error);
            }
            return false;
        }
    }

    // Method to reset selection state (useful after successful updates)
    public void resetSelection() {
        clearSelection();
    }

    // Enhanced clear selection with button state management
    public void clearSelection() {
        selectedFileUri = null;
        selectedFileName = null;
        if (currentUploadButton != null) {
            // Reset button text based on context
            String currentText = currentUploadButton.getText().toString();
            if (currentText.contains("Current:")) {
                // This is an update button, restore the current file display
                String currentFile = extractCurrentFileName(currentText);
                if (currentFile != null) {
                    currentUploadButton.setText("Current: " + currentFile + " (Tap to change)");
                } else {
                    currentUploadButton.setText("Select Document");
                }
            } else {
                currentUploadButton.setText("Select Document");
            }
        }
    }

    // Helper method to extract current filename from button text
    private String extractCurrentFileName(String buttonText) {
        try {
            if (buttonText.contains("Current: ") && buttonText.contains(" (Tap")) {
                int start = buttonText.indexOf("Current: ") + 9;
                int end = buttonText.indexOf(" (Tap");
                return buttonText.substring(start, end);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not extract filename from button text", e);
        }
        return null;
    }

    // Method to get detailed file selection status for UI updates
    public FileSelectionStatus getSelectionStatus() {
        return new FileSelectionStatus(
                selectedFileUri != null,
                selectedFileName,
                selectedFileUri != null ? getSelectedFileSize() : 0,
                selectedFileUri != null ? FileUtils.getMimeType(fragment.getContext(), selectedFileUri) : null
        );
    }

    // Class to hold file selection status
    public static class FileSelectionStatus {
        public final boolean hasFile;
        public final String fileName;
        public final long fileSize;
        public final String mimeType;

        public FileSelectionStatus(boolean hasFile, String fileName, long fileSize, String mimeType) {
            this.hasFile = hasFile;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
        }

        public String getFormattedSize() {
            if (fileSize < 1024) return fileSize + " B";
            int exp = (int) (Math.log(fileSize) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp - 1) + "B";
            return String.format("%.1f %s", fileSize / Math.pow(1024, exp), pre);
        }
    }

    // Enhanced upload progress callback interface
    public interface UploadProgressCallback {
        void onProgressUpdate(int progress);
        void onUploadComplete();
        void onUploadFailed(String error);
    }

    // Method to upload with progress tracking
    public void uploadWithProgress(String category, Map<String, String> formData,
                                   UploadProgressCallback progressCallback) {
        this.uploadCallback = new UploadCallback() {
            @Override
            public void onUploadSuccess(String message) {
                progressCallback.onUploadComplete();
            }

            @Override
            public void onUploadFailure(String error) {
                progressCallback.onUploadFailed(error);
            }
        };

        // Set up progress listener in uploadFile method
        uploadAndSave(category, formData);
    }
}