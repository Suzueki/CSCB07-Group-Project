package com.b07group32.relationsafe;

import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ItemViewerFragment extends Fragment {
    private static final String TAG = "ItemViewerFragment";
    private static final String ARG_ITEM_KEY = "item_key";
    private static final String ARG_ITEM_TITLE = "item_title";
    private static final String ARG_CATEGORY = "category";

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseRef;
    private DatabaseReference itemRef;
    private DocumentUploadManager uploadManager;
    private StorageReference storageRef;

    private String itemKey;
    private String itemTitle;
    private String category;

    // Current file data
    private String currentBlobReference;
    private String currentFileName;
    private String currentFileSize;
    private String currentMimeType;
    private Uri currentDownloadUrl;

    // UI elements
    private TextView titleTextView;
    private LinearLayout fieldsContainer;
    private LinearLayout fileSection;
    private TextView fileInfoTextView;

    // Document viewers
    private ImageView imageView;
    private WebView documentWebView;
    private ProgressBar loadingProgressBar;

    private Button downloadButton;
    private Button selectFileButton;
    private Button updateButton;
    private Button backButton;
    private Button copyLinkButton;
    private Button openExternalButton;
    private TextView downloadLinkTextView;

    // Dynamic field storage
    private Map<String, EditText> fieldEditTexts = new HashMap<>();

    public static ItemViewerFragment newInstance(String itemKey, String itemTitle, String category) {
        ItemViewerFragment fragment = new ItemViewerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ITEM_KEY, itemKey);
        args.putString(ARG_ITEM_TITLE, itemTitle);
        args.putString(ARG_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            itemKey = getArguments().getString(ARG_ITEM_KEY);
            itemTitle = getArguments().getString(ARG_ITEM_TITLE);
            category = getArguments().getString(ARG_CATEGORY);
        }

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            databaseRef = FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(currentUser.getUid())
                    .child("emergency_info");

            itemRef = databaseRef.child(category).child(itemKey);
            storageRef = FirebaseStorage.getInstance().getReference();

            // Initialize DocumentUploadManager
            uploadManager = new DocumentUploadManager(this, currentUser, databaseRef);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_viewer_fragment_native, container, false);

        if (currentUser == null) {
            Toast.makeText(getContext(), "Authentication error. Returning to previous screen.", Toast.LENGTH_LONG).show();
            getParentFragmentManager().popBackStack();
            return view;
        }

        initializeViews(view);
        setupCategorySpecificFields();
        setupButtons();
        loadItemData();

        return view;
    }

    private void initializeViews(View view) {
        titleTextView = view.findViewById(R.id.itemTitleTextView);
        fieldsContainer = view.findViewById(R.id.fieldsContainer);
        fileSection = view.findViewById(R.id.fileSection);
        fileInfoTextView = view.findViewById(R.id.fileNameTextView);

        // Document viewers
        imageView = view.findViewById(R.id.imageView);
        documentWebView = view.findViewById(R.id.documentWebView);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);

        downloadButton = view.findViewById(R.id.downloadButton);
        selectFileButton = view.findViewById(R.id.selectFileButton);
        updateButton = view.findViewById(R.id.updateButton);
        backButton = view.findViewById(R.id.backButton);
        copyLinkButton = view.findViewById(R.id.copyLinkButton);
        openExternalButton = view.findViewById(R.id.openExternalButton);
        downloadLinkTextView = view.findViewById(R.id.downloadStatusTextView);

        if (copyLinkButton != null) {
            copyLinkButton.setOnClickListener(v -> copyDownloadLinkToClipboard());
        }

        if (openExternalButton != null) {
            openExternalButton.setOnClickListener(v -> openFileExternally());
        }

        titleTextView.setText(category + " Viewer");

        // Show/hide file section based on category
        if ("documents".equals(category)) {
            fileSection.setVisibility(View.VISIBLE);
            setupWebView();
        } else {
            fileSection.setVisibility(View.GONE);
        }
    }

    private void setupWebView() {
        documentWebView.getSettings().setJavaScriptEnabled(false);
        documentWebView.getSettings().setBuiltInZoomControls(true);
        documentWebView.getSettings().setDisplayZoomControls(false);
        documentWebView.getSettings().setSupportZoom(true);
        documentWebView.getSettings().setLoadWithOverviewMode(true);
        documentWebView.getSettings().setUseWideViewPort(true);

        documentWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "WebView finished loading: " + url);
            }
        });
    }

    private void loadDocumentFileData(DataSnapshot dataSnapshot) {
        currentBlobReference = dataSnapshot.child("blobReference").getValue(String.class);
        currentFileName = dataSnapshot.child("originalFileName").getValue(String.class);
        currentFileSize = dataSnapshot.child("fileSize").getValue(String.class);
        currentMimeType = dataSnapshot.child("mimeType").getValue(String.class);

        if (currentBlobReference != null && currentFileName != null) {
            // Display file info
            String fileSizeText = formatFileSize(currentFileSize);
            fileInfoTextView.setText("Current file: " + currentFileName + " (" + fileSizeText + ")");

            // Load document using Firebase Storage directly
            loadDocumentFromStorage();
        } else {
            fileInfoTextView.setText("No file uploaded");
            downloadButton.setEnabled(false);
            copyLinkButton.setVisibility(View.GONE);
            openExternalButton.setVisibility(View.GONE);
            downloadLinkTextView.setVisibility(View.GONE);
            showNoDocumentMessage();
        }
    }

    private void loadDocumentFromStorage() {
        if (currentBlobReference == null) return;

        showLoadingState();

        StorageReference fileRef = storageRef.child(currentBlobReference);

        // Get download URL for sharing/downloading
        fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                currentDownloadUrl = uri;
                downloadButton.setEnabled(true);
                copyLinkButton.setVisibility(View.VISIBLE);
                openExternalButton.setVisibility(View.VISIBLE);
                downloadLinkTextView.setVisibility(View.VISIBLE);
                downloadLinkTextView.setText("✅ File ready for download and sharing");
                downloadLinkTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Failed to get download URL", e);
                downloadButton.setEnabled(false);
                copyLinkButton.setVisibility(View.GONE);
                openExternalButton.setVisibility(View.GONE);
                downloadLinkTextView.setVisibility(View.VISIBLE);
                downloadLinkTextView.setText("❌ Error loading download options");
                downloadLinkTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        });

        // Determine file type and load accordingly
        String fileType = determineFileType(currentFileName, currentMimeType);

        switch (fileType) {
            case "pdf":
                loadPdfAsWebView(fileRef);
                break;
            case "image":
                loadImageFromStorage(fileRef);
                break;
            default:
                hideLoadingState();
                showUnsupportedFileMessage(currentFileName, fileType);
                break;
        }
    }

    private void loadPdfAsWebView(StorageReference fileRef) {
        // For PDFs, we'll create a simple viewer that shows file info and provides options
        hideLoadingState();
        showWebViewer();

        String pdfViewerHtml = "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { padding:20px; text-align:center; background-color:#e8f5e8; border:2px solid #4caf50; border-radius:12px; margin:15px; font-family:Arial,sans-serif; }" +
                ".icon { font-size:64px; margin-bottom:20px; }" +
                "h3 { color:#2e7d32; margin:15px 0; }" +
                "p { color:#388e3c; margin:10px 0; line-height:1.5; }" +
                ".filename { font-weight:bold; color:#1b5e20; background:#c8e6c9; padding:8px 12px; border-radius:6px; margin:10px 0; display:inline-block; }" +
                ".action-text { color:#4caf50; font-size:14px; margin-top:15px; }" +
                "</style>" +
                "</head><body>" +
                "<div class='icon'>📄</div>" +
                "<h3>PDF Document Ready</h3>" +
                "<div class='filename'>" + currentFileName + "</div>" +
                "<p>This PDF document is ready to view.</p>" +
                "<p><strong>Use the buttons below to:</strong></p>" +
                "<p>• Download to your device</p>" +
                "<p>• Open in external PDF viewer</p>" +
                "<p>• Copy shareable link</p>" +
                "<div class='action-text'>PDF files are best viewed in dedicated PDF apps for full functionality.</div>" +
                "</body></html>";

        documentWebView.loadData(pdfViewerHtml, "text/html", "UTF-8");
    }

    private void loadImageFromStorage(StorageReference fileRef) {
        showImageViewer();

        // Download image to cache and display
        try {
            File localFile = File.createTempFile("temp_image", ".jpg", getContext().getCacheDir());

            fileRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<com.google.firebase.storage.FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(com.google.firebase.storage.FileDownloadTask.TaskSnapshot taskSnapshot) {
                    hideLoadingState();

                    // Load bitmap and display
                    Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        Log.d(TAG, "Image loaded successfully");
                    } else {
                        showErrorMessage("Image Error", "Failed to decode image file.");
                    }

                    // Clean up temp file
                    localFile.delete();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Failed to load image from storage", e);
                    hideLoadingState();
                    showErrorMessage("Image Load Error", "Unable to download image file for viewing.");
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Failed to create temp file for image", e);
            hideLoadingState();
            showErrorMessage("File Error", "Unable to create temporary file for image viewing.");
        }
    }

    private void showLoadingState() {
        hideAllViewers();
        loadingProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoadingState() {
        loadingProgressBar.setVisibility(View.GONE);
    }

    private void showImageViewer() {
        hideAllViewers();
        imageView.setVisibility(View.VISIBLE);
    }

    private void showWebViewer() {
        hideAllViewers();
        documentWebView.setVisibility(View.VISIBLE);
    }

    private void hideAllViewers() {
        imageView.setVisibility(View.GONE);
        documentWebView.setVisibility(View.GONE);
    }

    private void showNoDocumentMessage() {
        showWebViewer();
        String noDocHtml = "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { padding:20px; text-align:center; background-color:#e3f2fd; border:2px solid #90caf9; border-radius:12px; margin:15px; font-family:Arial,sans-serif; }" +
                ".icon { font-size:48px; margin-bottom:15px; }" +
                "h3 { color:#1565c0; margin:10px 0; }" +
                "p { color:#1976d2; margin:8px 0; line-height:1.4; }" +
                "</style>" +
                "</head><body>" +
                "<div class='icon'>📄</div>" +
                "<h3>No Document</h3>" +
                "<p>No document has been uploaded for this item.</p>" +
                "<p><strong>Select a file to upload and view.</strong></p>" +
                "</body></html>";
        documentWebView.loadData(noDocHtml, "text/html", "UTF-8");
    }

    private void showErrorMessage(String title, String message) {
        showWebViewer();
        String errorHtml = "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { padding:20px; text-align:center; background-color:#ffebee; border:2px solid #ef9a9a; border-radius:12px; margin:15px; font-family:Arial,sans-serif; }" +
                ".icon { font-size:48px; margin-bottom:15px; }" +
                "h3 { color:#c62828; margin:10px 0; }" +
                "p { color:#d32f2f; margin:8px 0; line-height:1.4; }" +
                "</style>" +
                "</head><body>" +
                "<div class='icon'>⚠️</div>" +
                "<h3>" + title + "</h3>" +
                "<p>" + message + "</p>" +
                "<p><strong>Try downloading the file to view it externally.</strong></p>" +
                "</body></html>";
        documentWebView.loadData(errorHtml, "text/html", "UTF-8");
    }

    private void showUnsupportedFileMessage(String fileName, String fileType) {
        showWebViewer();
        String extension = getFileExtension(fileName).toUpperCase();
        String unsupportedHtml = "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { padding:20px; text-align:center; background-color:#fff3cd; border:2px solid #ffeaa7; border-radius:12px; margin:15px; font-family:Arial,sans-serif; }" +
                ".icon { font-size:48px; margin-bottom:15px; }" +
                "h3 { color:#856404; margin:10px 0; }" +
                "p { color:#856404; margin:8px 0; line-height:1.4; }" +
                ".file-type { font-weight:bold; color:#d4800d; background:#ffeaa7; padding:4px 8px; border-radius:4px; }" +
                "</style>" +
                "</head><body>" +
                "<div class='icon'>📄</div>" +
                "<h3>Preview Not Available</h3>" +
                "<p>File: <strong>" + fileName + "</strong></p>" +
                "<p>Type: <span class='file-type'>" + extension + "</span></p>" +
                "<p>This file type cannot be previewed in the app.</p>" +
                "<p><strong>Use the buttons below to download or open externally.</strong></p>" +
                "</body></html>";
        documentWebView.loadData(unsupportedHtml, "text/html", "UTF-8");
    }

    private String determineFileType(String fileName, String mimeType) {
        // Check MIME type first
        if (mimeType != null) {
            if (mimeType.equals("application/pdf")) {
                return "pdf";
            } else if (mimeType.startsWith("image/")) {
                return "image";
            }
        }

        // Fallback to file extension
        if (fileName != null) {
            String extension = getFileExtension(fileName).toLowerCase();
            switch (extension) {
                case "pdf":
                    return "pdf";
                case "jpg":
                case "jpeg":
                case "png":
                case "gif":
                case "webp":
                case "bmp":
                    return "image";
                default:
                    return "unsupported";
            }
        }

        return "unsupported";
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private String formatFileSize(String fileSizeStr) {
        if (fileSizeStr == null || fileSizeStr.isEmpty()) {
            return "Unknown size";
        }

        try {
            long bytes = Long.parseLong(fileSizeStr);
            if (bytes < 1024) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp - 1) + "B";
            return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
        } catch (NumberFormatException e) {
            return "Unknown size";
        }
    }

    private void setupCategorySpecificFields() {
        fieldsContainer.removeAllViews();
        fieldEditTexts.clear();

        switch (category) {
            case "documents":
                addField("title", "Title", false);
                addField("description", "Description", true);
                break;
            case "contacts":
                addField("name", "Name", false);
                addField("phone", "Phone Number", false);
                addField("email", "Email", false);
                addField("relationship", "Relationship", false);
                addField("notes", "Notes", true);
                break;
            case "locations":
                addField("name", "Location Name", false);
                addField("address", "Address", true);
                addField("phone", "Phone Number", false);
                addField("notes", "Notes", true);
                break;
            case "medications":
                addField("name", "Medication Name", false);
                addField("dosage", "Dosage", false);
                addField("frequency", "Frequency", false);
                addField("prescriber", "Prescriber", false);
                addField("notes", "Notes", true);
                break;
        }
    }

    private void addField(String key, String label, boolean multiline) {
        // Create label
        TextView labelView = new TextView(getContext());
        labelView.setText(label);
        labelView.setTextSize(16f);
        labelView.setTextColor(getResources().getColor(android.R.color.black));
        labelView.setPadding(0, 0, 0, 8);

        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.setMargins(0, 16, 0, 0);
        labelView.setLayoutParams(labelParams);

        // Create EditText
        EditText editText = new EditText(getContext());
        editText.setHint("Enter " + label.toLowerCase());
        editText.setPadding(12, 12, 12, 12);
        editText.setTextSize(14f);

        if (multiline) {
            editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            editText.setMinLines(3);
            editText.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        }

        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        editParams.setMargins(0, 0, 0, 8);
        editText.setLayoutParams(editParams);

        fieldsContainer.addView(labelView);
        fieldsContainer.addView(editText);

        fieldEditTexts.put(key, editText);
    }

    private void setupButtons() {
        if ("documents".equals(category)) {
            String[] allowedMimeTypes = {"application/pdf", "image/*"};
            uploadManager.setupUploadButton(selectFileButton, allowedMimeTypes);
            downloadButton.setOnClickListener(v -> downloadDocument());
        }

        updateButton.setOnClickListener(v -> updateItem());
        backButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void loadItemData() {
        itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "Loading item data for: " + itemKey);

                    // Load field data
                    for (Map.Entry<String, EditText> entry : fieldEditTexts.entrySet()) {
                        String key = entry.getKey();
                        EditText editText = entry.getValue();
                        String value = dataSnapshot.child(key).getValue(String.class);
                        editText.setText(value != null ? value : "");
                    }

                    // Load file data for documents
                    if ("documents".equals(category)) {
                        loadDocumentFileData(dataSnapshot);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading item data", error.toException());
                Toast.makeText(getContext(), "Error loading " + category + ": " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadDocument() {
        if (currentDownloadUrl != null && currentFileName != null) {
            try {
                DownloadManager downloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);

                DownloadManager.Request request = new DownloadManager.Request(currentDownloadUrl);
                request.setTitle("Downloading " + currentFileName);
                request.setDescription("Emergency Info Document");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, currentFileName);
                request.setAllowedOverMetered(true);
                request.setAllowedOverRoaming(true);

                if (downloadManager != null) {
                    downloadManager.enqueue(request);
                    Toast.makeText(getContext(), "Download started. Check your Downloads folder.", Toast.LENGTH_LONG).show();
                } else {
                    openFileExternally();
                }
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                Toast.makeText(getContext(), "Download failed. Opening file instead.", Toast.LENGTH_SHORT).show();
                openFileExternally();
            }
        }
    }

    private void copyDownloadLinkToClipboard() {
        if (currentDownloadUrl != null) {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Document Download Link", currentDownloadUrl.toString());
            clipboard.setPrimaryClip(clip);

            String fileName = currentFileName != null ? currentFileName : "document";
            Toast.makeText(getContext(), "📋 Download link copied to clipboard!\n(" + fileName + ")", Toast.LENGTH_SHORT).show();

            Log.d(TAG, "Download link copied to clipboard: " + currentDownloadUrl.toString());
        } else {
            Toast.makeText(getContext(), "❌ Download link not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFileExternally() {
        if (currentDownloadUrl != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, currentDownloadUrl);

            if (currentMimeType != null && !currentMimeType.isEmpty()) {
                intent.setType(currentMimeType);
            } else {
                String extension = getFileExtension(currentFileName).toLowerCase();
                switch (extension) {
                    case "pdf":
                        intent.setType("application/pdf");
                        break;
                    case "jpg":
                    case "jpeg":
                        intent.setType("image/jpeg");
                        break;
                    case "png":
                        intent.setType("image/png");
                        break;
                    case "gif":
                        intent.setType("image/gif");
                        break;
                    case "webp":
                        intent.setType("image/webp");
                        break;
                    default:
                        intent.setType("*/*");
                        break;
                }
            }

            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Unable to open file externally", e);
                Toast.makeText(getContext(), "Unable to open file. Please check if you have a suitable app installed.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateItem() {
        if (!validateFields()) {
            return;
        }

        updateButton.setEnabled(false);
        updateButton.setText("Updating...");

        Map<String, String> formData = getFieldUpdates();

        if ("documents".equals(category) && uploadManager.hasSelectedFile()) {
            uploadManager.updateExistingItem(itemRef, formData, currentBlobReference, new DocumentUploadManager.UploadCallback() {
                @Override
                public void onUploadSuccess(String message) {
                    Toast.makeText(getContext(), "Document updated successfully!", Toast.LENGTH_SHORT).show();
                    resetUpdateButton();
                    loadItemData();
                }

                @Override
                public void onUploadFailure(String error) {
                    Toast.makeText(getContext(), "Update failed: " + error, Toast.LENGTH_LONG).show();
                    resetUpdateButton();
                }
            });
        } else {
            updateDatabaseOnly(formData);
        }
    }

    private boolean validateFields() {
        switch (category) {
            case "documents":
                if (fieldEditTexts.get("title").getText().toString().trim().isEmpty()) {
                    Toast.makeText(getContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    return false;
                }
                break;
            case "contacts":
                if (fieldEditTexts.get("name").getText().toString().trim().isEmpty()) {
                    Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    return false;
                }
                break;
            case "locations":
                if (fieldEditTexts.get("name").getText().toString().trim().isEmpty()) {
                    Toast.makeText(getContext(), "Location name cannot be empty", Toast.LENGTH_SHORT).show();
                    return false;
                }
                break;
            case "medications":
                if (fieldEditTexts.get("name").getText().toString().trim().isEmpty()) {
                    Toast.makeText(getContext(), "Medication name cannot be empty", Toast.LENGTH_SHORT).show();
                    return false;
                }
                break;
        }
        return true;
    }

    private void updateDatabaseOnly(Map<String, String> formData) {
        formData.put("timestamp", String.valueOf(System.currentTimeMillis()));

        itemRef.updateChildren((Map) formData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), category + " updated successfully", Toast.LENGTH_SHORT).show();
                    resetUpdateButton();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update database", e);
                    Toast.makeText(getContext(), "Failed to update " + category + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetUpdateButton();
                });
    }

    private Map<String, String> getFieldUpdates() {
        Map<String, String> updates = new HashMap<>();
        for (Map.Entry<String, EditText> entry : fieldEditTexts.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getText().toString().trim();
            updates.put(key, value);
        }
        return updates;
    }

    private void resetUpdateButton() {
        updateButton.setEnabled(true);
        updateButton.setText("Update");
    }

    @Override
    public void onResume() {
        super.onResume();
        currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            getParentFragmentManager().popBackStack();
        }
    }
}