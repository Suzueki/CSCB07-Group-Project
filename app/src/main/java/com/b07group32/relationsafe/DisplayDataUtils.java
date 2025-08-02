package com.b07group32.relationsafe;

import com.google.firebase.database.DataSnapshot;


public class DisplayDataUtils
{

    public static String buildDisplayString(DataSnapshot itemSnapshot, String category)
    {
        // Check if older content fields exist first
        String content = itemSnapshot.child("content").getValue(String.class);
        if (content != null)
        {
            return content;
        }

        // Construct structured display string
        String name = itemSnapshot.child("name").getValue(String.class);
        if (name == null){return null;}

        switch (category)
        {
            case "documents":
                return buildDocumentDisplay(itemSnapshot, name);
            case "medications":
                return buildMedicationDisplay(itemSnapshot, name);
            default:
                return name;
        }
    }

    private static String buildDocumentDisplay(DataSnapshot itemSnapshot, String name)
    {
        StringBuilder display = new StringBuilder(name);

        String type = itemSnapshot.child("type").getValue(String.class);
        if (type != null && !type.isEmpty())
        {
            display.append(" (").append(type).append(")");
        }

        String description = itemSnapshot.child("description").getValue(String.class);
        if (description != null && !description.isEmpty())
        {
            display.append(" - ").append(description);
        }

        String fileName = itemSnapshot.child("fileName").getValue(String.class);
        if (fileName != null)
        {
            display.append(" [File: ").append(fileName).append("]");
        }

        return display.toString();
    }

    private static String buildMedicationDisplay(DataSnapshot itemSnapshot, String name)
    {
        StringBuilder display = new StringBuilder(name);

        String dosage = itemSnapshot.child("dosage").getValue(String.class);
        if (dosage != null && !dosage.isEmpty())
        {
            display.append(" - ").append(dosage);
        }

        String frequency = itemSnapshot.child("frequency").getValue(String.class);
        if (frequency != null && !frequency.isEmpty())
        {
            display.append(", ").append(frequency);
        }

        String prescribedBy = itemSnapshot.child("prescribed_by").getValue(String.class);
        if (prescribedBy != null && !prescribedBy.isEmpty())
        {
            display.append(" (by ").append(prescribedBy).append(")");
        }

        String notes = itemSnapshot.child("notes").getValue(String.class);
        if (notes != null && !notes.isEmpty())
        {
            display.append(" - ").append(notes);
        }

        String fileName = itemSnapshot.child("fileName").getValue(String.class);
        if (fileName != null)
        {
            display.append(" [File: ").append(fileName).append("]");
        }

        return display.toString();
    }
}
