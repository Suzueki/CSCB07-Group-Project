package com.b07group32.relationsafe;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.util.HashMap;
import java.util.Map;

public class FormBuilder
{

    public static class DocumentForm
    {
        public LinearLayout layout;
        public EditText nameField, typeField, descriptionField;
        public Button uploadButton;
    }

    public static class MedicationForm
    {
        public LinearLayout layout;
        public EditText nameField, dosageField, frequencyField, prescribedByField, notesField;
        public Button uploadButton;
    }

    public static class ContactForm
    {
        public LinearLayout layout;
        public EditText nameField, relationshipField, phoneField;
        public Button uploadButton; // Add this field
    }

    public static class LocationForm
    {
        public LinearLayout layout;
        public EditText nameField, addressField, notesField;
        public Button uploadButton; // Add this field
    }

    public static DocumentForm createDocumentForm(Context context)
    {
        DocumentForm form = new DocumentForm();
        form.layout = createBaseLayout(context);

        form.nameField = createEditText(context, "Document Name", "name");
        form.typeField = createEditText(context, "Document Type (e.g., ID, Court Order)", "type");
        form.descriptionField = createEditText(context, "Description (optional)", "description");
        form.uploadButton = new Button(context);
        form.uploadButton.setText("Upload File (Image/PDF)");

        form.layout.addView(form.nameField);
        form.layout.addView(form.typeField);
        form.layout.addView(form.descriptionField);
        form.layout.addView(form.uploadButton);

        return form;
    }

    public static MedicationForm createMedicationForm(Context context)
    {
        MedicationForm form = new MedicationForm();
        form.layout = createBaseLayout(context);

        form.nameField = createEditText(context, "Medication Name", "name");
        form.dosageField = createEditText(context, "Dosage (e.g., 10mg)", "dosage");
        form.frequencyField = createEditText(context, "Frequency (e.g., Twice daily)", "frequency");
        form.prescribedByField = createEditText(context, "Prescribed by", "prescribed_by");
        form.notesField = createEditText(context, "Notes (optional)", "notes");
        form.uploadButton = new Button(context);
        form.uploadButton.setText("Upload Prescription/Photo");

        form.layout.addView(form.nameField);
        form.layout.addView(form.dosageField);
        form.layout.addView(form.frequencyField);
        form.layout.addView(form.prescribedByField);
        form.layout.addView(form.notesField);
        form.layout.addView(form.uploadButton);

        return form;
    }

    public static ContactForm createContactForm(Context context)
    {
        ContactForm form = new ContactForm();
        form.layout = createBaseLayout(context);

        form.nameField = createEditText(context, "Contact Name", "name");
        form.relationshipField = createEditText(context, "Relationship (e.g., Friend, Family)", "relationship");
        form.phoneField = createEditText(context, "Phone Number", "phone");
        form.phoneField.setInputType(InputType.TYPE_CLASS_PHONE);

        form.layout.addView(form.nameField);
        form.layout.addView(form.relationshipField);
        form.layout.addView(form.phoneField);

        return form;
    }

    public static LocationForm createLocationForm(Context context)
    {
        LocationForm form = new LocationForm();
        form.layout = createBaseLayout(context);

        form.nameField = createEditText(context, "Location Name", "name");
        form.addressField = createEditText(context, "Address", "address");
        form.notesField = createEditText(context, "Notes (optional)", "notes");

        form.layout.addView(form.nameField);
        form.layout.addView(form.addressField);
        form.layout.addView(form.notesField);

        return form;
    }

    private static LinearLayout createBaseLayout(Context context)
    {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);
        return layout;
    }

    private static EditText createEditText(Context context, String hint, String tag)
    {
        EditText editText = new EditText(context);
        editText.setHint(hint);
        editText.setTag(tag);
        return editText;
    }

    public static Map<String, String> extractFormData(LinearLayout layout)
    {
        Map<String, String> formData = new HashMap<>();

        for (int i = 0; i < layout.getChildCount(); i++)
        {
            View child = layout.getChildAt(i);
            if (child instanceof EditText)
            {
                EditText editText = (EditText) child;
                String tag = (String) editText.getTag();
                String value = editText.getText().toString().trim();
                if (tag != null)
                {
                    formData.put(tag, value);
                }
            }
        }

        return formData;
    }

    public static ContactForm createContactFormWithUpload(Context context)
    {
        ContactForm form = new ContactForm();
        form.layout = createBaseLayout(context);

        form.nameField = createEditText(context, "Contact Name", "name");
        form.relationshipField = createEditText(context, "Relationship (e.g., Friend, Family)", "relationship");
        form.phoneField = createEditText(context, "Phone Number", "phone");
        form.phoneField.setInputType(InputType.TYPE_CLASS_PHONE);

        // Add upload button for contact photos/vCards
        form.uploadButton = new Button(context);
        form.uploadButton.setText("Upload Photo/Contact File (Optional)");

        form.layout.addView(form.nameField);
        form.layout.addView(form.relationshipField);
        form.layout.addView(form.phoneField);
        form.layout.addView(form.uploadButton);

        return form;
    }

    public static LocationForm createLocationFormWithUpload(Context context)
    {
        LocationForm form = new LocationForm();
        form.layout = createBaseLayout(context);

        form.nameField = createEditText(context, "Location Name", "name");
        form.addressField = createEditText(context, "Address", "address");
        form.notesField = createEditText(context, "Notes (optional)", "notes");

        // Add upload button for location photos/maps
        form.uploadButton = new Button(context);
        form.uploadButton.setText("Upload Photo/Map (Optional)");

        form.layout.addView(form.nameField);
        form.layout.addView(form.addressField);
        form.layout.addView(form.notesField);
        form.layout.addView(form.uploadButton);

        return form;
    }

    public static MedicationForm createMedicationFormWithUpload(Context context)
    {
        MedicationForm form = new MedicationForm();
        form.layout = createBaseLayout(context);

        form.nameField = createEditText(context, "Medication Name", "name");
        form.dosageField = createEditText(context, "Dosage (e.g., 10mg)", "dosage");
        form.frequencyField = createEditText(context, "Frequency (e.g., Twice daily)", "frequency");
        form.prescribedByField = createEditText(context, "Prescribed by", "prescribed_by");
        form.notesField = createEditText(context, "Notes (optional)", "notes");

        // Upload button already exists in original - just ensure it's added
        form.uploadButton = new Button(context);
        form.uploadButton.setText("Upload Prescription/Photo (Optional)");

        form.layout.addView(form.nameField);
        form.layout.addView(form.dosageField);
        form.layout.addView(form.frequencyField);
        form.layout.addView(form.prescribedByField);
        form.layout.addView(form.notesField);
        form.layout.addView(form.uploadButton);

        return form;
    }
}