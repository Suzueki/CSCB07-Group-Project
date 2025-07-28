package com.b07group32.relationsafe;
import com.google.gson.*;
import com.google.gson.internal.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class RuntimeTypeAdapterFactory<T> implements TypeAdapterFactory {
    private final Class<?> baseType;
    private final String typeFieldName;
    private final Map<String, Class<?>> labelToSubtype = new LinkedHashMap<>();

    private RuntimeTypeAdapterFactory(Class<?> baseType, String typeFieldName) {
        this.baseType = baseType;
        this.typeFieldName = typeFieldName;
    }

    public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType, String typeFieldName) {
        return new RuntimeTypeAdapterFactory<>(baseType, typeFieldName);
    }

    public RuntimeTypeAdapterFactory<T> registerSubtype(Class<? extends T> subtype, String label) {
        labelToSubtype.put(label, subtype);
        return this;
    }

    @Override
    public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> type) {
        if (!baseType.isAssignableFrom(type.getRawType())) return null;

        final Map<String, TypeAdapter<?>> subtypeAdapters = new LinkedHashMap<>();
        for (Map.Entry<String, Class<?>> entry : labelToSubtype.entrySet()) {
            TypeAdapter<?> adapter = gson.getDelegateAdapter(this, TypeToken.get(entry.getValue()));
            subtypeAdapters.put(entry.getKey(), adapter);
        }

        return new TypeAdapter<R>() {
            @Override
            public void write(JsonWriter out, R value) throws IOException {
                Class<?> subclass = value.getClass();
                String label = null;
                for (Map.Entry<String, Class<?>> entry : labelToSubtype.entrySet()) {
                    if (entry.getValue().equals(subclass)) {
                        label = entry.getKey();
                        break;
                    }
                }
                if (label == null) throw new JsonParseException("Subtype not registered: " + subclass);

                TypeAdapter<R> delegate = (TypeAdapter<R>) subtypeAdapters.get(label);
                JsonObject jsonObj = delegate.toJsonTree(value).getAsJsonObject();
                jsonObj.addProperty(typeFieldName, label);
                Streams.write(jsonObj, out);
            }

            @Override
            public R read(JsonReader in) throws IOException {
                JsonElement element = Streams.parse(in);
                JsonObject jsonObj = element.getAsJsonObject();
                JsonElement typeElement = jsonObj.get(typeFieldName);
                if (typeElement == null)
                    throw new JsonParseException("Missing type field: " + typeFieldName);
                String label = typeElement.getAsString();

                TypeAdapter<?> delegate = subtypeAdapters.get(label);
                if (delegate == null)
                    throw new JsonParseException("Subtype not registered for label: " + label);
                return (R) delegate.fromJsonTree(jsonObj);
            }
        };
    }
}
