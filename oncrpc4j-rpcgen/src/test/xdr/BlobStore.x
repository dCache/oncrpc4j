struct Key {
    opaque data<16>;
};

union Value switch (bool notNull) {
    case TRUE:
        opaque data<1024>;
    case FALSE:
        void;
};

program BLOB_STORAGE {
    version BLOB_STORAGE_VERS {
        void put(Key key, Value value) = 1;
        Value get(Key key) = 2;
    } = 1;
} = 118;