print('Start #################################################################');

db = db.getSiblingDB('integration-main');
db.createUser(
    {
        "user": "admin",
        "pwd": "admin",
        "roles": [{ role: "readWrite", db: "integration-main" }]
    },
);

db = db.getSiblingDB('integration-test');
db.createUser(
    {
        "user": "admin",
        "pwd": "admin",
        "roles": [{ role: "readWrite", db: "integration-test" }]
    },
);

print('END #################################################################');