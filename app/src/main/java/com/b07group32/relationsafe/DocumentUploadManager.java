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
                                Log.d(TAG, "File selected: " + selectedFileName);
                            }
                            else
                            {
                                selectedFileUri = null;
                                selectedFileName = null;
                                Toast.makeText(fragment.getContext(),
                                        "File is too large. Maximum size is 10MB.",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                    else
                    {
                        Log.w(TAG, "File selection cancelled or failed");
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

    private void openFilePicker(String[] mimeTypes)
    {//ask Brandan(?) if we need this metadata because the requirement docs seemed less like metadata
        //and more like general info
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
            Toast.makeText(fragment.getContext(), "Please install a file manager", Toast.LENGTH_SHORT).show();
        }
    }

    public void uploadAndSave(String category, Map<String, String> formData)
    {
        Log.d(TAG, "uploadAndSave called for category: " + category);

        if (selectedFileUri != null)
        {
            Log.d(TAG, "Uploading blob to Storage, metadata to Realtime DB: " + selectedFileName);
            uploadFile(category, formData, selectedFileUri);
        }
        else
        {
            Log.d(TAG, "No file selected, saving form data only to Realtime DB");
            saveMetadata(category, formData, null, null);
        }
    }

    private void uploadFile(String category, Map<String, String> formData, Uri fileUri)
    {
        if (currentUser == null)
        {
            Log.e(TAG, "User not authenticated");
            Toast.makeText(fragment.getContext(), "Authentication error", Toast.LENGTH_SHORT).show();
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

                Toast.makeText(fragment.getContext(), "File uploaded successfully!", Toast.LENGTH_SHORT).show();

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
            });

        }
        catch (Exception e)
        {
            Log.e(TAG, "Exception during blob upload setup", e);
            Toast.makeText(fragment.getContext(), "Error preparing upload: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    //Delete this function and the above if 'metadata' turns out to only be descriptions
    private void saveMetadataWithBlobRef(String category, Map<String, String> formData, String blobReference, String originalFileName, String blobId)
    {
        if (currentUser == null)
        {
            Log.e(TAG, "User not authenticated for metadata save");
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
                        Toast.makeText(fragment.getContext(), "Item with file added successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                    {
                        Log.e(TAG, "Failed to save file metadata to Realtime Database", e);
                        Toast.makeText(fragment.getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
        else
        {
            Log.e(TAG, "Failed to generate database key");
            Toast.makeText(fragment.getContext(), "Database error: Could not generate key", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveMetadata(String category, Map<String, String> formData, String downloadUrl, String fileName)
    {
        if (currentUser == null)
        {
            Log.e(TAG, "User not authenticated for metadata save");
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
                        Toast.makeText(fragment.getContext(), "Item added successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                    {
                        Log.e(TAG, "Failed to save metadata to Realtime Database", e);
                        Toast.makeText(fragment.getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
        else
        {
            Log.e(TAG, "Failed to generate database key");
            Toast.makeText(fragment.getContext(), "Database error: Could not generate key", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to check if a file is currently selected
    // use later for metadata and be more robust
    public boolean hasSelectedFile() {
        return selectedFileUri != null;
    }

    // Method to get selected file name
    public String getSelectedFileName()
    {
        return selectedFileName;
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

    // Method to clear current selection
    public void clearSelection()
    {
        selectedFileUri = null;
        selectedFileName = null;
        if (currentUploadButton != null)
        {
            currentUploadButton.setText("Select Document");
        }
    }
}