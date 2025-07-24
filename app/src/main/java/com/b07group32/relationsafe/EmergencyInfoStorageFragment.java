package com.b07group32.relationsafe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EmergencyInfoStorageFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    // File upload related
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Uri selectedFileUri;
    private String selectedFileName;
    private Button currentUploadButton;

    private RecyclerView documentsListView, contactsListView, locationsListView, medicationsListView;
    private Button buttonAddDocument, buttonAddContact, buttonAddLocation, buttonAddMedication;
    private Button buttonDeleteDocument, buttonDeleteContact, buttonDeleteLocation, buttonDeleteMedication;
    private Button buttonBack;

    private List<String> documentsList = new ArrayList<>();
    private List<String> contactsList = new ArrayList<>();
    private List<String> locationsList = new ArrayList<>();
    private List<String> medicationsList = new ArrayList<>();

    private List<String> documentsKeys = new ArrayList<>();
    private List<String> contactsKeys = new ArrayList<>();
    private List<String> locationsKeys = new ArrayList<>();
    private List<String> medicationsKeys = new ArrayList<>();

    private SimpleItemAdapter documentsAdapter, contactsAdapter, locationsAdapter, medicationsAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Storage
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Initialize file picker launcher
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedFileUri = result.getData().getData();
                        selectedFileName = getFileName(selectedFileUri);
                        if (currentUploadButton != null) {
                            currentUploadButton.setText("Selected: " + selectedFileName);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.emergency_info_storage_fragment, container, false);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getContext(), "Authentication error. Returning to home.", Toast.LENGTH_LONG).show();
            getParentFragmentManager().popBackStack();
            return view;
        }

        databaseRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(currentUser.getUid()).child("emergency_info");

        initializeViews(view);
        setupRecyclerViews();
        setupButtons();
        loadDataFromFirebase();

        return view;
    }

    private void initializeViews(View view) {
        documentsListView = view.findViewById(R.id.documentsListView);
        contactsListView = view.findViewById(R.id.contactsListView);
        locationsListView = view.findViewById(R.id.locationsListView);
        medicationsListView = view.findViewById(R.id.medicationsListView);

        buttonAddDocument = view.findViewById(R.id.buttonAddDocument);
        buttonAddContact = view.findViewById(R.id.buttonAddContact);
        buttonAddLocation = view.findViewById(R.id.buttonAddLocation);
        buttonAddMedication = view.findViewById(R.id.buttonAddMedication);

        buttonDeleteDocument = view.findViewById(R.id.buttonDeleteDocument);
        buttonDeleteContact = view.findViewById(R.id.buttonDeleteContact);
        buttonDeleteLocation = view.findViewById(R.id.buttonDeleteLocation);
        buttonDeleteMedication = view.findViewById(R.id.buttonDeleteMedication);

        buttonBack = view.findViewById(R.id.buttonBack);
    }

    private void setupRecyclerViews() {
        documentsAdapter = new SimpleItemAdapter(documentsList, pos -> {
            buttonDeleteDocument.setEnabled(true);
            clearOtherSelections(documentsAdapter);
        });
        contactsAdapter = new SimpleItemAdapter(contactsList, pos -> {
            buttonDeleteContact.setEnabled(true);
            clearOtherSelections(contactsAdapter);
        });
        locationsAdapter = new SimpleItemAdapter(locationsList, pos -> {
            buttonDeleteLocation.setEnabled(true);
            clearOtherSelections(locationsAdapter);
        });
        medicationsAdapter = new SimpleItemAdapter(medicationsList, pos -> {
            buttonDeleteMedication.setEnabled(true);
            clearOtherSelections(medicationsAdapter);
        });

        documentsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        documentsListView.setAdapter(documentsAdapter);

        contactsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        contactsListView.setAdapter(contactsAdapter);

        locationsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        locationsListView.setAdapter(locationsAdapter);

        medicationsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        medicationsListView.setAdapter(medicationsAdapter);
    }

    private void clearOtherSelections(SimpleItemAdapter exceptAdapter) {
        if (exceptAdapter != documentsAdapter) {
            documentsAdapter.clearSelection();
            buttonDeleteDocument.setEnabled(false);
        }
        if (exceptAdapter != contactsAdapter) {
            contactsAdapter.clearSelection();
            buttonDeleteContact.setEnabled(false);
        }
        if (exceptAdapter != locationsAdapter) {
            locationsAdapter.clearSelection();
            buttonDeleteLocation.setEnabled(false);
        }
        if (exceptAdapter != medicationsAdapter) {
            medicationsAdapter.clearSelection();
            buttonDeleteMedication.setEnabled(false);
        }
    }

    private void setupButtons() {
        buttonAddDocument.setOnClickListener(v -> showAddDocumentDialog());
        buttonAddContact.setOnClickListener(v -> showAddContactDialog());
        buttonAddLocation.setOnClickListener(v -> showAddLocationDialog());
        buttonAddMedication.setOnClickListener(v -> showAddMedicationDialog());

        buttonDeleteDocument.setOnClickListener(v -> {
            int pos = documentsAdapter.getSelectedPosition();
            if (pos != RecyclerView.NO_POSITION) {
                showDeleteConfirmDialog("Document", () -> deleteItemFromFirebase("documents", pos));
            }
        });
        buttonDeleteContact.setOnClickListener(v -> {
            int pos = contactsAdapter.getSelectedPosition();
            if (pos != RecyclerView.NO_POSITION) {
                showDeleteConfirmDialog("Emergency Contact", () -> deleteItemFromFirebase("contacts", pos));
            }
        });
        buttonDeleteLocation.setOnClickListener(v -> {
            int pos = locationsAdapter.getSelectedPosition();
            if (pos != RecyclerView.NO_POSITION) {
                showDeleteConfirmDialog("Safe Location", () -> deleteItemFromFirebase("locations", pos));
            }
        });
        buttonDeleteMedication.setOnClickListener(v -> {
            int pos = medicationsAdapter.getSelectedPosition();
            if (pos != RecyclerView.NO_POSITION) {
                showDeleteConfirmDialog("Medication", () -> deleteItemFromFirebase("medications", pos));
            }
        });

        buttonBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void loadCategoryData(String category, List<String> list, SimpleItemAdapter adapter, List<String> keys) {
        databaseRef.child(category).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list.clear();
                keys.clear();
                for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                    // Try to get the old "content" field first for backwards compatibility
                    String content = itemSnapshot.child("content").getValue(String.class);

                    if (content == null) {
                        // New format - build display string from structured data
                        String name = itemSnapshot.child("name").getValue(String.class);
                        if (name != null) {
                            content = buildDisplayString(itemSnapshot, category);
                        }
                    }

                    if (content != null) {
                        list.add(content);
                        keys.add(itemSnapshot.getKey());
                    }
                }
                adapter.notifyDataSetChanged();
                adapter.clearSelection();
                disableAllDeleteButtons();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load " + category + ": " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String buildDisplayString(DataSnapshot itemSnapshot, String category) {
        String name = itemSnapshot.child("name").getValue(String.class);
        if (name == null) return null;

        switch (category) {
            case "documents":
                String type = itemSnapshot.child("type").getValue(String.class);
                String description = itemSnapshot.child("description").getValue(String.class);
                String fileName = itemSnapshot.child("fileName").getValue(String.class);

                StringBuilder docDisplay = new StringBuilder(name);
                if (type != null && !type.isEmpty()) {
                    docDisplay.append(" (").append(type).append(")");
                }
                if (description != null && !description.isEmpty()) {
                    docDisplay.append(" - ").append(description);
                }
                if (fileName != null) {
                    docDisplay.append(" [File: ").append(fileName).append("]");
                }
                return docDisplay.toString();

            case "medications":
                String dosage = itemSnapshot.child("dosage").getValue(String.class);
                String frequency = itemSnapshot.child("frequency").getValue(String.class);
                String prescribedBy = itemSnapshot.child("prescribed_by").getValue(String.class);
                String notes = itemSnapshot.child("notes").getValue(String.class);
                String medFileName = itemSnapshot.child("fileName").getValue(String.class);

                StringBuilder medDisplay = new StringBuilder(name);
                if (dosage != null && !dosage.isEmpty()) {
                    medDisplay.append(" - ").append(dosage);
                }
                if (frequency != null && !frequency.isEmpty()) {
                    medDisplay.append(", ").append(frequency);
                }
                if (prescribedBy != null && !prescribedBy.isEmpty()) {
                    medDisplay.append(" (by ").append(prescribedBy).append(")");
                }
                if (notes != null && !notes.isEmpty()) {
                    medDisplay.append(" - ").append(notes);
                }
                if (medFileName != null) {
                    medDisplay.append(" [File: ").append(medFileName).append("]");
                }
                return medDisplay.toString();

            default:
                return name;
        }
    }

    private void disableAllDeleteButtons() {
        buttonDeleteDocument.setEnabled(false);
        buttonDeleteContact.setEnabled(false);
        buttonDeleteLocation.setEnabled(false);
        buttonDeleteMedication.setEnabled(false);
    }

    private void loadDataFromFirebase() {
        loadCategoryData("documents", documentsList, documentsAdapter, documentsKeys);
        loadCategoryData("contacts", contactsList, contactsAdapter, contactsKeys);
        loadCategoryData("locations", locationsList, locationsAdapter, locationsKeys);
        loadCategoryData("medications", medicationsList, medicationsAdapter, medicationsKeys);
    }

    private void showDeleteConfirmDialog(String itemType, Runnable deleteAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Delete " + itemType);
        builder.setMessage("Are you sure you want to delete this " + itemType.toLowerCase() + "?");
        builder.setPositiveButton("Delete", (dialog, which) -> deleteAction.run());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddDocumentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Document");

        LinearLayout dialogView = new LinearLayout(getContext());
        dialogView.setOrientation(LinearLayout.VERTICAL);
        dialogView.setPadding(50, 30, 50, 30);

        EditText etDocumentName = new EditText(getContext());
        etDocumentName.setHint("Document Name");
        etDocumentName.setTag("name");

        EditText etDocumentType = new EditText(getContext());
        etDocumentType.setHint("Document Type (e.g., ID, Court Order)");
        etDocumentType.setTag("type");

        EditText etDescription = new EditText(getContext());
        etDescription.setHint("Description (optional)");
        etDescription.setTag("description");

        Button btnUploadFile = new Button(getContext());
        btnUploadFile.setText("Upload File (Image/PDF)");
        currentUploadButton = btnUploadFile;

        dialogView.addView(etDocumentName);
        dialogView.addView(etDocumentType);
        dialogView.addView(etDescription);
        dialogView.addView(btnUploadFile);

        // Reset selected file when dialog opens
        selectedFileUri = null;
        selectedFileName = null;

        btnUploadFile.setOnClickListener(v -> openFilePicker(new String[]{"image/*", "application/pdf"}));

        builder.setView(dialogView);
        builder.setPositiveButton("Add", (dialog, which) -> {
            Map<String, String> formData = extractFormData(dialogView);
            String name = formData.get("name");

            if (!name.isEmpty()) {
                if (selectedFileUri != null) {
                    uploadDocumentToFirebase("documents", formData, selectedFileUri);
                } else {
                    saveDocumentMetadata("documents", formData, null, null);
                }
            } else {
                Toast.makeText(getContext(), "Please enter document name", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddMedicationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Medication");

        LinearLayout dialogView = new LinearLayout(getContext());
        dialogView.setOrientation(LinearLayout.VERTICAL);
        dialogView.setPadding(50, 30, 50, 30);

        EditText etMedicationName = new EditText(getContext());
        etMedicationName.setHint("Medication Name");
        etMedicationName.setTag("name");

        EditText etDosage = new EditText(getContext());
        etDosage.setHint("Dosage (e.g., 10mg)");
        etDosage.setTag("dosage");

        EditText etFrequency = new EditText(getContext());
        etFrequency.setHint("Frequency (e.g., Twice daily)");
        etFrequency.setTag("frequency");

        EditText etPrescribedBy = new EditText(getContext());
        etPrescribedBy.setHint("Prescribed by");
        etPrescribedBy.setTag("prescribed_by");

        EditText etNotes = new EditText(getContext());
        etNotes.setHint("Notes (optional)");
        etNotes.setTag("notes");

        Button btnUploadFile = new Button(getContext());
        btnUploadFile.setText("Upload Prescription/Photo");
        currentUploadButton = btnUploadFile;

        dialogView.addView(etMedicationName);
        dialogView.addView(etDosage);
        dialogView.addView(etFrequency);
        dialogView.addView(etPrescribedBy);
        dialogView.addView(etNotes);
        dialogView.addView(btnUploadFile);

        // Reset selected file when dialog opens
        selectedFileUri = null;
        selectedFileName = null;

        btnUploadFile.setOnClickListener(v -> openFilePicker(new String[]{"image/*", "application/pdf"}));

        builder.setView(dialogView);
        builder.setPositiveButton("Add", (dialog, which) -> {
            Map<String, String> formData = extractFormData(dialogView);
            String name = formData.get("name");
            String dosage = formData.get("dosage");

            if (!name.isEmpty() && !dosage.isEmpty()) {
                if (selectedFileUri != null) {
                    uploadDocumentToFirebase("medications", formData, selectedFileUri);
                } else {
                    saveDocumentMetadata("medications", formData, null, null);
                }
            } else {
                Toast.makeText(getContext(), "Please enter medication name and dosage", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Keep existing contact and location dialogs unchanged
    private void showAddContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Emergency Contact");

        LinearLayout dialogView = new LinearLayout(getContext());
        dialogView.setOrientation(LinearLayout.VERTICAL);
        dialogView.setPadding(50, 30, 50, 30);

        EditText etContactName = new EditText(getContext());
        etContactName.setHint("Contact Name");
        EditText etRelationship = new EditText(getContext());
        etRelationship.setHint("Relationship (e.g., Friend, Family)");
        EditText etPhone = new EditText(getContext());
        etPhone.setHint("Phone Number");
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);

        dialogView.addView(etContactName);
        dialogView.addView(etRelationship);
        dialogView.addView(etPhone);

        builder.setView(dialogView);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = etContactName.getText().toString().trim();
            String relationship = etRelationship.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (!name.isEmpty() && !phone.isEmpty()) {
                String contactInfo = name + " (" + relationship + ") - " + phone;
                addItemToFirebase("contacts", contactInfo);
            } else {
                Toast.makeText(getContext(), "Please enter name and phone number", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Safe Location");

        LinearLayout dialogView = new LinearLayout(getContext());
        dialogView.setOrientation(LinearLayout.VERTICAL);
        dialogView.setPadding(50, 30, 50, 30);

        EditText etLocationName = new EditText(getContext());
        etLocationName.setHint("Location Name");
        EditText etAddress = new EditText(getContext());
        etAddress.setHint("Address");
        EditText etNotes = new EditText(getContext());
        etNotes.setHint("Notes (optional)");

        dialogView.addView(etLocationName);
        dialogView.addView(etAddress);
        dialogView.addView(etNotes);

        builder.setView(dialogView);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = etLocationName.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();

            if (!name.isEmpty() && !address.isEmpty()) {
                String locationInfo = name + " - " + address + (notes.isEmpty() ? "" : " (" + notes + ")");
                addItemToFirebase("locations", locationInfo);
            } else {
                Toast.makeText(getContext(), "Please enter location name and address", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // File upload helper methods
    private void openFilePicker(String[] mimeTypes) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            filePickerLauncher.launch(Intent.createChooser(intent, "Select Document"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), "Please install a file manager", Toast.LENGTH_SHORT).show();
        }
    }

    private Map<String, String> extractFormData(LinearLayout dialogView) {
        Map<String, String> formData = new HashMap<>();

        for (int i = 0; i < dialogView.getChildCount(); i++) {
            View child = dialogView.getChildAt(i);
            if (child instanceof EditText) {
                EditText editText = (EditText) child;
                String tag = (String) editText.getTag();
                String value = editText.getText().toString().trim();
                if (tag != null) {
                    formData.put(tag, value);
                }
            }
        }

        return formData;
    }

    private void uploadDocumentToFirebase(String category, Map<String, String> formData, Uri fileUri) {
        Toast.makeText(getContext(), "Uploading...", Toast.LENGTH_SHORT).show();

        String fileExtension = getFileExtension(fileUri);
        String fileName = UUID.randomUUID().toString() + "." + fileExtension;

        // Use category-specific storage path under user's emergency_info
        StorageReference documentRef = storageRef
                .child("users")
                .child(currentUser.getUid())
                .child("emergency_info")
                .child(category)
                .child(fileName);

        documentRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    documentRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        saveDocumentMetadata(category, formData, downloadUri.toString(), fileName);
                        Toast.makeText(getContext(), "File uploaded successfully!", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveDocumentMetadata(String category, Map<String, String> formData, String downloadUrl, String fileName) {
        // Add common fields
        formData.put("timestamp", String.valueOf(System.currentTimeMillis()));
        if (downloadUrl != null) {
            formData.put("downloadUrl", downloadUrl);
            formData.put("fileName", fileName);
        }

        String itemId = databaseRef.child(category).push().getKey();

        if (itemId != null) {
            databaseRef.child(category).child(itemId).setValue(formData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Item added successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getFileExtension(Uri uri) {
        String fileName = getFileName(uri);
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }

        String mimeType = getContext().getContentResolver().getType(uri);
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                return mimeType.substring(6);
            } else if (mimeType.equals("application/pdf")) {
                return "pdf";
            }
        }
        return "unknown";
    }

    // Keep existing methods unchanged
    private void addItemToFirebase(String category, String item) {
        if (!isUserAuthenticated()) return;

        String key = databaseRef.child(category).push().getKey();
        if (key != null) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("content", item);
            itemData.put("timestamp", System.currentTimeMillis());

            databaseRef.child(category).child(key).setValue(itemData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Item added successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to add item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void deleteItemFromFirebase(String category, int position) {
        if (!isUserAuthenticated()) return;

        List<String> keys = getKeysForCategory(category);
        if (position >= 0 && position < keys.size()) {
            String key = keys.get(position);

            // Get the item data first to check if it has a file to delete
            databaseRef.child(category).child(key).get().addOnSuccessListener(dataSnapshot -> {
                String fileName = dataSnapshot.child("fileName").getValue(String.class);

                // Delete the database entry
                databaseRef.child(category).child(key).removeValue()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Item deleted successfully", Toast.LENGTH_SHORT).show();
                            clearAllSelections();

                            // Delete the file from storage if it exists
                            if (fileName != null) {
                                StorageReference fileRef = storageRef
                                        .child("users")
                                        .child(currentUser.getUid())
                                        .child("emergency_info")
                                        .child(category)
                                        .child(fileName);

                                fileRef.delete().addOnFailureListener(e -> {
                                    // File deletion failed, but database entry is already deleted
                                    // This is not critical, so we just log it
                                    Toast.makeText(getContext(), "File deleted from database but not from storage", Toast.LENGTH_SHORT).show();
                                });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to delete item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        }
    }

    private List<String> getKeysForCategory(String category) {
        switch (category) {
            case "documents": return documentsKeys;
            case "contacts": return contactsKeys;
            case "locations": return locationsKeys;
            case "medications": return medicationsKeys;
            default: return new ArrayList<>();
        }
    }

    private void clearAllSelections() {
        documentsAdapter.clearSelection();
        contactsAdapter.clearSelection();
        locationsAdapter.clearSelection();
        medicationsAdapter.clearSelection();

        buttonDeleteDocument.setEnabled(false);
        buttonDeleteContact.setEnabled(false);
        buttonDeleteLocation.setEnabled(false);
        buttonDeleteMedication.setEnabled(false);
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

    private boolean isUserAuthenticated() {
        currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to access this feature", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return false;
        }
        return true;
    }
}