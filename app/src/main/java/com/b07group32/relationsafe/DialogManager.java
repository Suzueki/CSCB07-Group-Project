package com.b07group32.relationsafe;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Map;

class DialogManager
{
    private EmergencyInfoStorageFragment fragment;
    private DocumentUploadManager uploadManager;
    private BiConsumer<String, String> simpleItemCallback;

    public DialogManager(EmergencyInfoStorageFragment fragment, DocumentUploadManager uploadManager, BiConsumer<String, String> simpleItemCallback)
    {
        this.fragment = fragment;
        this.uploadManager = uploadManager;
        this.simpleItemCallback = simpleItemCallback;
    }

    public void showDocumentDialog()
    {
        FormBuilder.DocumentForm form = FormBuilder.createDocumentForm(fragment.getContext());
        String[] allowedMimeTypes = {"image/*", "application/pdf"};
        showItemDialog("Add Document", "documents", "Document", form.layout, form.uploadButton, allowedMimeTypes);
    }

    public void showMedicationDialog()
    {
        FormBuilder.MedicationForm form = FormBuilder.createMedicationFormWithUpload(fragment.getContext());
        String[] allowedMimeTypes = {"image/*", "application/pdf"};
        showItemDialog("Add Medication", "medications", "Medication", form.layout, form.uploadButton, allowedMimeTypes);
    }

    public void showContactDialog()
    {
        FormBuilder.ContactForm form = FormBuilder.createContactFormWithUpload(fragment.getContext());
        String[] allowedMimeTypes = {"image/*"};
        showItemDialog("Add Contact", "contacts", "Contact", form.layout, form.uploadButton, allowedMimeTypes);
    }

    public void showLocationDialog()
    {
        FormBuilder.LocationForm form = FormBuilder.createLocationFormWithUpload(fragment.getContext());
        String[] allowedMimeTypes = {"image/*", "application/pdf"};
        showItemDialog("Add Location", "locations", "Location", form.layout, form.uploadButton, allowedMimeTypes);
    }

    private void showItemDialog(String title, String itemType, String itemDisplayName,
                                LinearLayout layout, Button uploadButton, String[] allowedMimeTypes)
    {
        AlertDialog dialog = new AlertDialog.Builder(fragment.getContext())
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface ->
        {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            uploadManager.setupUploadButton(uploadButton, allowedMimeTypes);

            positiveButton.setOnClickListener(view ->
            {
                Map<String, String> formData = FormBuilder.extractFormData(layout);
                if (validateRequired("name", formData.get("name")))
                {
                    if (uploadManager.hasSelectedFile())
                    {
                        uploadManager.addNewItemWithFile(itemType, formData, createUploadCallback(itemDisplayName, dialog));
                    }
                    else
                    {
                        uploadManager.addNewItemWithoutFile(itemType, formData, createUploadCallback(itemDisplayName, dialog));
                    }
                }
            });
        });

        dialog.show();
    }

    private DocumentUploadManager.UploadCallback createUploadCallback(String itemDisplayName, AlertDialog dialog)
    {
        return new DocumentUploadManager.UploadCallback()
        {
            @Override
            public void onUploadSuccess(String message)
            {
                Toast.makeText(fragment.getContext(), itemDisplayName + " added successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }

            @Override
            public void onUploadFailure(String error)
            {
                String errorMessage;
                if (uploadManager.hasSelectedFile())
                {
                    errorMessage = "Upload failed: " + error;
                }
                else
                {
                    errorMessage = "Failed to add " + itemDisplayName.toLowerCase() + ": " + error;
                }
                Toast.makeText(fragment.getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        };
    }

    private boolean validateRequired(String fieldName, String... fields)
    {
        for (String field : fields)
        {
            if (field == null || field.trim().isEmpty())
            {
                String message;
                if (fieldName.equals("name"))
                {
                    message = "Please provide a proper name";
                }
                else
                {
                    message = "Please fill in all required fields";
                }
                Toast.makeText(fragment.getContext(), message, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    public static void showDeleteConfirmDialog(Context context, String itemType, Runnable deleteAction) {
        new AlertDialog.Builder(context)
                .setTitle("Delete " + itemType)
                .setMessage("Are you sure you want to delete this " + itemType.toLowerCase() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAction.run())
                .setNegativeButton("Cancel", null)
                .show();
    }
}