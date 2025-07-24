package com.b07group32.relationsafe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;


public class EmergencyInfoStorageFragment extends Fragment
{

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseRef;

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

    // refactored!
    private DocumentUploadManager documentUploadManager;
    private CategoryDataManager categoryDataManager;
    private DialogManager dialogManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser != null)
        {
            databaseRef = FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(currentUser.getUid())
                    .child("emergency_info");

            // initialize managers
            documentUploadManager = new DocumentUploadManager(this, currentUser, databaseRef);
            categoryDataManager = new CategoryDataManager(databaseRef);
            dialogManager = new DialogManager(this, documentUploadManager, this::addItemToFirebase);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.emergency_info_storage_fragment, container, false);

        if (currentUser == null)
        {
            Toast.makeText(getContext(), "Authentication error. Returning to home.", Toast.LENGTH_LONG).show();
            getParentFragmentManager().popBackStack();
            return view;
        }

        //part of refactor, NEED THESE FOR FEATURES
        initializeViews(view);
        setupRecyclerViews();
        setupButtons();
        loadDataFromFirebase();

        return view;
    }

    private void initializeViews(View view)
    {
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

    private void setupRecyclerViews()
    {
        documentsAdapter = new SimpleItemAdapter(documentsList, pos -> selectItem("documents", pos));
        contactsAdapter = new SimpleItemAdapter(contactsList, pos -> selectItem("contacts", pos));
        locationsAdapter = new SimpleItemAdapter(locationsList, pos -> selectItem("locations", pos));
        medicationsAdapter = new SimpleItemAdapter(medicationsList, pos -> selectItem("medications", pos));

        documentsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        documentsListView.setAdapter(documentsAdapter);
        contactsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        contactsListView.setAdapter(contactsAdapter);
        locationsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        locationsListView.setAdapter(locationsAdapter);
        medicationsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        medicationsListView.setAdapter(medicationsAdapter);
    }

    private void selectItem(String category, int position)
    {
        clearAllSelections();
        switch (category)
        {
            case "documents":
                documentsAdapter.setSelectedPosition(position);
                buttonDeleteDocument.setEnabled(true);
                break;
            case "contacts":
                contactsAdapter.setSelectedPosition(position);
                buttonDeleteContact.setEnabled(true);
                break;
            case "locations":
                locationsAdapter.setSelectedPosition(position);
                buttonDeleteLocation.setEnabled(true);
                break;
            case "medications":
                medicationsAdapter.setSelectedPosition(position);
                buttonDeleteMedication.setEnabled(true);
                break;
        }
    }

    private void setupButtons()
    {
        buttonAddDocument.setOnClickListener(v -> dialogManager.showDocumentDialog());
        buttonAddContact.setOnClickListener(v -> dialogManager.showContactDialog());
        buttonAddLocation.setOnClickListener(v -> dialogManager.showLocationDialog());
        buttonAddMedication.setOnClickListener(v -> dialogManager.showMedicationDialog());

        buttonDeleteDocument.setOnClickListener(v -> deleteSelectedItem("documents", documentsAdapter));
        buttonDeleteContact.setOnClickListener(v -> deleteSelectedItem("contacts", contactsAdapter));
        buttonDeleteLocation.setOnClickListener(v -> deleteSelectedItem("locations", locationsAdapter));
        buttonDeleteMedication.setOnClickListener(v -> deleteSelectedItem("medications", medicationsAdapter));

        buttonBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void deleteSelectedItem(String category, SimpleItemAdapter adapter)
    {
        int pos = adapter.getSelectedPosition();
        if (pos != RecyclerView.NO_POSITION) {
            DialogManager.showDeleteConfirmDialog(getContext(), category,
                    () -> deleteItemFromFirebase(category, pos));
        }
    }

    private void loadDataFromFirebase()
    {
        categoryDataManager.loadCategoryData("documents", documentsList, documentsAdapter, documentsKeys, this::disableAllDeleteButtons);
        categoryDataManager.loadCategoryData("contacts", contactsList, contactsAdapter, contactsKeys, this::disableAllDeleteButtons);
        categoryDataManager.loadCategoryData("locations", locationsList, locationsAdapter, locationsKeys, this::disableAllDeleteButtons);
        categoryDataManager.loadCategoryData("medications", medicationsList, medicationsAdapter, medicationsKeys, this::disableAllDeleteButtons);
    }

    private void clearAllSelections()
    {
        documentsAdapter.clearSelection();
        contactsAdapter.clearSelection();
        locationsAdapter.clearSelection();
        medicationsAdapter.clearSelection();
        disableAllDeleteButtons();
    }

    private void disableAllDeleteButtons()
    {
        buttonDeleteDocument.setEnabled(false);
        buttonDeleteContact.setEnabled(false);
        buttonDeleteLocation.setEnabled(false);
        buttonDeleteMedication.setEnabled(false);
    }

    // Simple method for backward compatibility (contacts & locations)
    private void addItemToFirebase(String category, String item)
    {
        if (!isUserAuthenticated()) return;

        categoryDataManager.addSimpleItem(category, item, getContext());
    }

    private void deleteItemFromFirebase(String category, int position)
    {
        if (!isUserAuthenticated()) return;

        List<String> keys = getKeysForCategory(category);
        if (position >= 0 && position < keys.size())
        {
            String key = keys.get(position);
            categoryDataManager.deleteItem(category, key, currentUser, getContext(), this::clearAllSelections);
        }
    }

    private List<String> getKeysForCategory(String category)
    {
        switch (category)
        {
            case "documents": return documentsKeys;
            case "contacts": return contactsKeys;
            case "locations": return locationsKeys;
            case "medications": return medicationsKeys;
            default: return new ArrayList<>();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        currentUser = auth.getCurrentUser();
        //if they somehow get past verification, send them back
        if (currentUser == null)
        {
            Toast.makeText(getContext(), "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            getParentFragmentManager().popBackStack();
        }
    }

    private boolean isUserAuthenticated()
    {
        currentUser = auth.getCurrentUser();
        if (currentUser == null)
        {
            Toast.makeText(getContext(), "Please log in to access this feature", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return false;
        }
        return true;
    }
}