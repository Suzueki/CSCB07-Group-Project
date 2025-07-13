package com.b07group32.relationsafe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Map;

public class AddItemFragment extends Fragment {
    private Spinner spinnerCategory;
    private LinearLayout dynamicFieldsContainer;
    private Button buttonAdd;
    private FirebaseDatabase db;
    private DatabaseReference itemsRef;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    // dynamic fields that will be created based on category of document
    private EditText field1, field2, field3, field4;
    private TextView label1, label2, label3, label4;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_item, container, false);

        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        dynamicFieldsContainer = view.findViewById(R.id.dynamicFieldsContainer);
        buttonAdd = view.findViewById(R.id.buttonAdd);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to add items", Toast.LENGTH_SHORT).show();
            return view;
        }

        db = FirebaseDatabase.getInstance("https://relationsafe-20cde-default-rtdb.firebaseio.com/");

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.categories_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = parent.getItemAtPosition(position).toString();
                updateFieldsForCategory(selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItem();
            }
        });

        return view;
    }

    private void updateFieldsForCategory(String category) {
        dynamicFieldsContainer.removeAllViews();

        switch (category.toLowerCase()) {
            case "documents":
                createDocumentFields();
                break;
            case "emergency contact":
                createEmergencyContactFields();
                break;
            case "safe location":
                createSafeLocationFields();
                break;
            case "medication":
                createMedicationFields();
                break;
            default:
                break;
        }
    }

    //placeholder for later
    private void createDocumentFields() {
        label1 = createLabel("File Name:");
        field1 = createEditText("Enter file name");

        label2 = createLabel("Short Note:");
        field2 = createEditText("Enter a short note");

        label3 = createLabel("PDF Path:");
        field3 = createEditText("PDF file path or URL");

        addFieldsToContainer(label1, field1, label2, field2, label3, field3);
    }

    private void createEmergencyContactFields() {
        label1 = createLabel("Person's Name:");
        field1 = createEditText("Enter person's name");

        label2 = createLabel("Phone Number:");
        field2 = createEditText("Enter phone number");

        addFieldsToContainer(label1, field1, label2, field2);
    }

    private void createSafeLocationFields() {
        label1 = createLabel("Address:");
        field1 = createEditText("Enter address");

        label2 = createLabel("Note:");
        field2 = createEditText("Enter a note about this location");

        addFieldsToContainer(label1, field1, label2, field2);
    }

    private void createMedicationFields() {
        label1 = createLabel("Drug Name:");
        field1 = createEditText("Enter drug name");

        label2 = createLabel("Dosage:");
        field2 = createEditText("Enter dosage");

        label3 = createLabel("Prescription:");
        field3 = createEditText("Enter prescription details");

        addFieldsToContainer(label1, field1, label2, field2, label3, field3);
    }

    private TextView createLabel(String text) {
        TextView label = new TextView(getContext());
        label.setText(text);
        label.setPadding(0, 16, 0, 8);
        return label;
    }

    private EditText createEditText(String hint) {
        EditText editText = new EditText(getContext());
        editText.setHint(hint);
        editText.setPadding(16, 16, 16, 16);
        return editText;
    }

    private void addFieldsToContainer(View... views) {
        for (View view : views) {
            if (view != null) {
                dynamicFieldsContainer.addView(view);
            }
        }
    }

    private void addItem() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to add items", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = spinnerCategory.getSelectedItem().toString().toLowerCase();

        if (!validateFields(category)) {
            Toast.makeText(getContext(), "Please fill out all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        itemsRef = db.getReference("users/" + userId + "/categories/" + category);

        String id = itemsRef.push().getKey();

        Object item = createItemForCategory(category, id);

        if (item != null) {
            buttonAdd.setEnabled(false);
            buttonAdd.setText("Adding...");

            itemsRef.child(id).setValue(item).addOnCompleteListener(task -> {
                buttonAdd.setEnabled(true);
                buttonAdd.setText("Add Item");

                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Item added successfully", Toast.LENGTH_SHORT).show();
                    clearFields();
                } else {
                    Toast.makeText(getContext(), "Failed to add item", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void deleteItem(String category, String itemId) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to delete items", Toast.LENGTH_SHORT).show();
            return;
        }

        if (itemId == null || itemId.isEmpty()) {
            Toast.makeText(getContext(), "Invalid item ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        itemsRef = db.getReference("users/" + userId + "/categories/" + category.toLowerCase());

        itemsRef.child(itemId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Item deleted successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to delete item", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void editItem(String category, String oldItemId, Object newItemData) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to edit items", Toast.LENGTH_SHORT).show();
            return;
        }

        if (oldItemId == null || oldItemId.isEmpty()) {
            Toast.makeText(getContext(), "Invalid item ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        itemsRef = db.getReference("users/" + userId + "/categories/" + category.toLowerCase());
        String newId = itemsRef.push().getKey();

        //add 'edited' item
        itemsRef.child(newId).setValue(newItemData).addOnCompleteListener(addTask -> {
            if (addTask.isSuccessful()) {
                //delete old item
                itemsRef.child(oldItemId).removeValue().addOnCompleteListener(deleteTask -> {
                    if (deleteTask.isSuccessful()) {
                        Toast.makeText(getContext(), "Item updated successfully", Toast.LENGTH_SHORT).show();
                        clearFields();
                    } else {
                        Toast.makeText(getContext(), "Item added but failed to remove old version", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(getContext(), "Failed to update item", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void loadItemForEdit(String category, String itemId) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to edit items", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        itemsRef = db.getReference("users/" + userId + "/categories/" + category.toLowerCase());

        itemsRef.child(itemId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                setSpinnerSelection(category);

                populateFieldsFromData(category, task.getResult().getValue());

                buttonAdd.setText("Update Item");
                buttonAdd.setOnClickListener(v -> saveItem(true, itemId));

            } else {
                Toast.makeText(getContext(), "Failed to load item for editing", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void getUserItems(String category, OnItemsLoadedListener listener) {
        if (currentUser == null) {
            listener.onError("Please log in to view items");
            return;
        }

        String userId = currentUser.getUid();
        itemsRef = db.getReference("users/" + userId + "/categories/" + category.toLowerCase());

        itemsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                listener.onItemsLoaded(task.getResult().getValue());
            } else {
                listener.onError("Failed to load items");
            }
        });
    }

    public interface OnItemsLoadedListener {
        void onItemsLoaded(Object items);
        void onError(String error);
    }

    private boolean validateFields(String category) {
        switch (category) {
            case "documents":
                return !field1.getText().toString().trim().isEmpty() &&
                        !field2.getText().toString().trim().isEmpty() &&
                        !field3.getText().toString().trim().isEmpty();
            case "emergency contact":
                return !field1.getText().toString().trim().isEmpty() &&
                        !field2.getText().toString().trim().isEmpty();
            case "safe location":
                return !field1.getText().toString().trim().isEmpty() &&
                        !field2.getText().toString().trim().isEmpty();
            case "medication":
                return !field1.getText().toString().trim().isEmpty() &&
                        !field2.getText().toString().trim().isEmpty() &&
                        !field3.getText().toString().trim().isEmpty();
            default:
                return false;
        }
    }

    private Object createItemForCategory(String category, String id) {
        switch (category) {
            case "documents":
                return new DocumentItem(id,
                        field1.getText().toString().trim(), // fileName
                        field2.getText().toString().trim(), // shortNote
                        field3.getText().toString().trim()  // pdfPath
                );
            case "emergency contact":
                return new EmergencyContactItem(id,
                        field1.getText().toString().trim(), // personName
                        field2.getText().toString().trim()  // phoneNumber
                );
            case "safe location":
                return new SafeLocationItem(id,
                        field1.getText().toString().trim(), // address
                        field2.getText().toString().trim()  // note
                );
            case "medication":
                return new MedicationItem(id,
                        field1.getText().toString().trim(), // drugName
                        field2.getText().toString().trim(), // dosage
                        field3.getText().toString().trim()  // prescription
                );
            default:
                return null;
        }
    }

    private void clearFields() {
        if (field1 != null) field1.setText("");
        if (field2 != null) field2.setText("");
        if (field3 != null) field3.setText("");
        if (field4 != null) field4.setText("");
    }

    private void setSpinnerSelection(String category) {
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerCategory.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(category)) {
                spinnerCategory.setSelection(i);
                break;
            }
        }
    }

    private void populateFieldsFromData(String category, Object data) {
        switch (category.toLowerCase()) {
            case "documents":
                if (data instanceof Map) {
                    Map<String, Object> docData = (Map<String, Object>) data;
                    if (field1 != null) field1.setText((String) docData.get("fileName"));
                    if (field2 != null) field2.setText((String) docData.get("shortNote"));
                    if (field3 != null) field3.setText((String) docData.get("pdfPath"));
                }
                break;
            case "emergency contact":
                if (data instanceof Map) {
                    Map<String, Object> contactData = (Map<String, Object>) data;
                    if (field1 != null) field1.setText((String) contactData.get("personName"));
                    if (field2 != null) field2.setText((String) contactData.get("phoneNumber"));
                }
                break;
            case "safe location":
                if (data instanceof Map) {
                    Map<String, Object> locationData = (Map<String, Object>) data;
                    if (field1 != null) field1.setText((String) locationData.get("address"));
                    if (field2 != null) field2.setText((String) locationData.get("note"));
                }
                break;
            case "medication":
                if (data instanceof Map) {
                    Map<String, Object> medData = (Map<String, Object>) data;
                    if (field1 != null) field1.setText((String) medData.get("drugName"));
                    if (field2 != null) field2.setText((String) medData.get("dosage"));
                    if (field3 != null) field3.setText((String) medData.get("prescription"));
                }
                break;
        }
    }

    private void saveItem(boolean isEditing, String oldItemId) {
        String category = spinnerCategory.getSelectedItem().toString().toLowerCase();

        // field validation
        if (!validateFields(category)) {
            Toast.makeText(getContext(), "Please fill out all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String newId = db.getReference("users/" + userId + "/categories/" + category).push().getKey();
        Object item = createItemForCategory(category, newId);

        if (item != null) {
            if (isEditing) {
                // se edit (add then delete)
                editItem(category, oldItemId, item);
            } else {
                //use standard add method
                addItem();
            }
        }
    }

    public void resetToAddMode() {
        clearFields();
        buttonAdd.setText("Add Item");
        buttonAdd.setOnClickListener(v -> addItem());
        if (spinnerCategory.getAdapter().getCount() > 0) {
            spinnerCategory.setSelection(0);
        }
    }
}