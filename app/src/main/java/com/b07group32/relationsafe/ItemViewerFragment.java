package com.b07group32.relationsafe;

import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
    private WebView documentWebView;
    private Button downloadButton;
    private Button selectFileButton;
    private Button updateButton;
    private Button backButton;
    private Button copyLinkButton;
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

            // Initialize DocumentUploadManager
            uploadManager = new DocumentUploadManager(this, currentUser, databaseRef);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_viewer_fragment, container, false);

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
        documentWebView = view.findViewById(R.id.documentWebView);
        downloadButton = view.findViewById(R.id.downloadButton);
        selectFileButton = view.findViewById(R.id.selectFileButton);
        updateButton = view.findViewById(R.id.updateButton);
        backButton = view.findViewById(R.id.backButton);

        // Get the copy button and status text from XML layout
        copyLinkButton = view.findViewById(R.id.copyLinkButton);
        downloadLinkTextView = view.findViewById(R.id.downloadStatusTextView);

        // Set up copy button click listener
        if (copyLinkButton != null) {
            copyLinkButton.setOnClickListener(v -> copyDownloadLinkToClipboard());
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

    private void showWebViewError(String title, String message) {
        String errorHtml = "<html><body style='padding:20px;text-align:center;background-color:#fff3cd;border:1px solid #ffeaa7;border-radius:8px;margin:10px;font-family:Arial,sans-serif;'>" +
                "<h3 style='color:#856404;margin-top:0;'>" + title + "</h3>" +
                "<p style='color:#856404;'>" + message + "</p>" +
                "</body></html>";
        documentWebView.loadData(errorHtml, "text/html", "UTF-8");
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
            // Use DocumentUploadManager for file selection
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

    private void loadDocumentFileData(DataSnapshot dataSnapshot) {
        currentBlobReference = dataSnapshot.child("blobReference").getValue(String.class);
        currentFileName = dataSnapshot.child("originalFileName").getValue(String.class);
        currentFileSize = dataSnapshot.child("fileSize").getValue(String.class);
        currentMimeType = dataSnapshot.child("mimeType").getValue(String.class);

        if (currentBlobReference != null && currentFileName != null) {
            // Display file info
            String fileSizeText = formatFileSize(currentFileSize);
            fileInfoTextView.setText("Current file: " + currentFileName + " (" + fileSizeText + ")");

            // Get download URL from blob reference
            uploadManager.getDownloadUrl(currentBlobReference, new DocumentUploadManager.DownloadUrlCallback() {
                @Override
                public void onSuccess(Uri downloadUrl) {
                    currentDownloadUrl = downloadUrl;
                    downloadButton.setEnabled(true);
                    copyLinkButton.setVisibility(View.VISIBLE);
                    downloadLinkTextView.setVisibility(View.VISIBLE);
                    downloadLinkTextView.setText("✅ File ready for download and sharing");
                    downloadLinkTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                    // Load document preview
                    loadDocumentPreview(downloadUrl, currentFileName, currentMimeType);
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Failed to get download URL: " + error);
                    fileInfoTextView.setText("Error loading file: " + currentFileName);
                    downloadButton.setEnabled(false);
                    copyLinkButton.setVisibility(View.GONE);
                    downloadLinkTextView.setVisibility(View.VISIBLE);
                    downloadLinkTextView.setText("❌ Error loading download options");
                    downloadLinkTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    showWebViewError("File Loading Error", "Unable to load the document file.");
                }
            });
        } else {
            fileInfoTextView.setText("No file uploaded");
            downloadButton.setEnabled(false);
            copyLinkButton.setVisibility(View.GONE);
            downloadLinkTextView.setVisibility(View.GONE);
            showWebViewError("No Document", "No document has been uploaded for this item. Select a file to upload and view.");
        }
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

    private void loadDocumentPreview(Uri downloadUrl, String fileName, String mimeType) {
        if (downloadUrl == null) {
            showWebViewError("Preview Error", "Cannot generate preview URL");
            return;
        }

        Log.d(TAG, "Loading preview for: " + fileName + " (MIME: " + mimeType + ")");

        // Determine file type from MIME type or filename
        String fileType = determineFileType(fileName, mimeType);

        switch (fileType) {
            case "pdf":
                loadPdfPreview(downloadUrl);
                break;
            case "image":
                loadImagePreview(downloadUrl, fileName);
                break;
            default:
                showUnsupportedFilePreview(fileName, fileType);
                break;
        }
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

    private void loadPdfPreview(Uri downloadUrl) {
        // Use Google Docs viewer for PDFs
        String pdfViewerUrl = "https://docs.google.com/gview?embedded=true&url=" + downloadUrl.toString();
        Log.d(TAG, "Loading PDF preview: " + pdfViewerUrl);
        documentWebView.loadUrl(pdfViewerUrl);
    }

    private void loadImagePreview(Uri downloadUrl, String fileName) {
        // Create responsive HTML for image display
        String imageHtml = "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { margin:0; padding:15px; text-align:center; background-color:#f8f9fa; font-family:Arial,sans-serif; }" +
                "img { max-width:100%; height:auto; border-radius:8px; box-shadow:0 4px 12px rgba(0,0,0,0.15); background:#fff; }" +
                ".filename { margin-bottom:10px; color:#666; font-size:14px; }" +
                "</style>" +
                "</head><body>" +
                "<div class='filename'>" + fileName + "</div>" +
                "<img src='" + downloadUrl.toString() + "' alt='Document Image' onclick='this.style.maxWidth=this.style.maxWidth===\"none\"?\"100%\":\"none\"'/>" +
                "<p style='margin-top:15px; color:#888; font-size:12px;'>Tap image to toggle full size</p>" +
                "</body></html>";

        Log.d(TAG, "Loading image preview for: " + fileName);
        documentWebView.loadData(imageHtml, "text/html", "UTF-8");
    }

    private void showUnsupportedFilePreview(String fileName, String fileType) {
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
                "<p><strong>Use the download button below to view this file externally.</strong></p>" +
                "</body></html>";

        Log.d(TAG, "Showing unsupported file preview for: " + fileName);
        documentWebView.loadData(unsupportedHtml, "text/html", "UTF-8");
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
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

            // Show confirmation with file name
            String fileName = currentFileName != null ? currentFileName : "document";
            Toast.makeText(getContext(), "📋 Download link copied to clipboard!\n(" + fileName + ")", Toast.LENGTH_SHORT).show();

            Log.d(TAG, "Download link copied to clipboard: " + currentDownloadUrl.toString());
        } else {
            Toast.makeText(getContext(), "❌ Download link not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDirectDownloadLink() {
        if (currentDownloadUrl != null) {
            openFileExternally();
        } else {
            Toast.makeText(getContext(), "Download link not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFileExternally() {
        if (currentDownloadUrl != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, currentDownloadUrl);

            // Set appropriate MIME type
            if (currentMimeType != null && !currentMimeType.isEmpty()) {
                intent.setType(currentMimeType);
            } else {
                // Fallback based on file extension
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
            // Update with file using DocumentUploadManager
            uploadManager.updateExistingItem(itemRef, formData, currentBlobReference, new DocumentUploadManager.UploadCallback() {
                @Override
                public void onUploadSuccess(String message) {
                    Toast.makeText(getContext(), "Document updated successfully!", Toast.LENGTH_SHORT).show();
                    resetUpdateButton();
                    // Reload data to show updated file
                    loadItemData();
                }

                @Override
                public void onUploadFailure(String error) {
                    Toast.makeText(getContext(), "Update failed: " + error, Toast.LENGTH_LONG).show();
                    resetUpdateButton();
                }
            });
        } else {
            // Update text fields only
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