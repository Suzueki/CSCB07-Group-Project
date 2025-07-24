package com.b07group32.relationsafe;


import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import java.util.Map;

class DialogManager {
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
        uploadManager.setupUploadButton(form.uploadButton, new String[]{"image/*", "application/pdf"});

        new AlertDialog.Builder(fragment.getContext())
                .setTitle("Add Document")
                .setView(form.layout)
                .setPositiveButton("Add", (dialog, which) ->
                {
                    Map<String, String> data = FormBuilder.extractFormData(form.layout);
                    if (validateRequired(data.get("name"))) {
                        uploadManager.uploadAndSave("documents", data);
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    public void showMedicationDialog() {
        FormBuilder.MedicationForm form = FormBuilder.createMedicationForm(fragment.getContext());
        uploadManager.setupUploadButton(form.uploadButton, new String[]{"image/*", "application/pdf"});

        new AlertDialog.Builder(fragment.getContext())
                .setTitle("Add Medication")
                .setView(form.layout)
                .setPositiveButton("Add", (dialog, which) ->
                {
                    Map<String, String> data = FormBuilder.extractFormData(form.layout);
                    if (validateRequired(data.get("name"), data.get("dosage")))
                    {
                        uploadManager.uploadAndSave("medications", data);
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    public void showContactDialog()
    {
        FormBuilder.ContactForm form = FormBuilder.createContactForm(fragment.getContext());

        new AlertDialog.Builder(fragment.getContext())
                .setTitle("Add Emergency Contact")
                .setView(form.layout)
                .setPositiveButton("Add", (dialog, which) ->
                {
                    String name = form.nameField.getText().toString().trim();
                    String relationship = form.relationshipField.getText().toString().trim();
                    String phone = form.phoneField.getText().toString().trim();

                    if (validateRequired(name, phone))
                    {
                        String contactInfo = name + " (" + relationship + ") - " + phone;
                        simpleItemCallback.accept("contacts", contactInfo);
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    public void showLocationDialog() {
        FormBuilder.LocationForm form = FormBuilder.createLocationForm(fragment.getContext());

        new AlertDialog.Builder(fragment.getContext())
                .setTitle("Add Safe Location")
                .setView(form.layout)
                .setPositiveButton("Add", (dialog, which) ->
                {
                    String name = form.nameField.getText().toString().trim();
                    String address = form.addressField.getText().toString().trim();
                    String notes = form.notesField.getText().toString().trim();

                    if (validateRequired(name, address))
                    {
                        String locationInfo = name + " - " + address + (notes.isEmpty() ? "" : " (" + notes + ")");
                        simpleItemCallback.accept("locations", locationInfo);
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    private boolean validateRequired(String... fields)
    {
        for (String field : fields)
        {
            if (field == null || field.trim().isEmpty())
            {
                Toast.makeText(fragment.getContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    public static void showDeleteConfirmDialog(Context context, String itemType, Runnable deleteAction)
    {
        new AlertDialog.Builder(context)
                .setTitle("Delete " + itemType)
                .setMessage("Are you sure you want to delete this " + itemType.toLowerCase() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAction.run())
                .setNegativeButton("Cancel", null)
                .show();
    }
}