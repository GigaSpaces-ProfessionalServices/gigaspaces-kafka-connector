package com.gigaspaces.kafka.connector;

import org.apache.kafka.common.config.ConfigDef;

public class GigaspacesSinkConnectorConfig {
    public static final String NAME                 = "gs.connector.name";
    public static final String GS_IS_EMBEDDED_SPACE = "gs.space.embedded";
    public static final String GS_SPACE_NAME        = "gs.space.name";
    public static final String GS_GROUPS            = "gs.space.groups";
    public static final String GS_LOCATORS          = "gs.space.locators";
    public static final String GS_USERNAME          = "gs.username";
    public static final String GS_PASSWORD          = "gs.password";
    public static final String GS_POJOS_PACKAGE     = "gs.pojos.package";
    public static final String GS_MODEL_JAR_PATH    = "gs.model.jar.path";
    public static final String GS_MODEL_JSON_PATH    = "gs.model.json.path";

    protected static ConfigDef configDef() {
        ConfigDef configDef = new ConfigDef();
        configDef.define(NAME, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Connector name");
        configDef.define(GS_IS_EMBEDDED_SPACE, ConfigDef.Type.BOOLEAN, ConfigDef.Importance.LOW, "Run Gigaspaces as embedded space (Debugging)");
        configDef.define(GS_SPACE_NAME, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Gigaspaces space name");
        configDef.define(GS_USERNAME, ConfigDef.Type.STRING,"", ConfigDef.Importance.LOW, "Username for access a secure space");
        configDef.define(GS_PASSWORD, ConfigDef.Type.STRING, "",ConfigDef.Importance.LOW, "Password for access a secure space");
        configDef.define(GS_GROUPS, ConfigDef.Type.STRING, "",ConfigDef.Importance.LOW, "Lookup groups for locating a space");
        configDef.define(GS_LOCATORS, ConfigDef.Type.STRING, "",ConfigDef.Importance.LOW, "Space lookup locator");
        configDef.define(GS_POJOS_PACKAGE, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Model POJOs package name");
        configDef.define(GS_MODEL_JAR_PATH, ConfigDef.Type.STRING, "", ConfigDef.Importance.HIGH, "Model POJOs jar full path");
        configDef.define(GS_MODEL_JSON_PATH, ConfigDef.Type.STRING, "", ConfigDef.Importance.HIGH, "Json model file full path");
        return configDef;
    }
}
