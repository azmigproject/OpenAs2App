package org.openas2.lib.dbUtils;

import org.joda.time.DateTime;

public class Multicommand {


    public String getDescription() {
        return _description;
    }
    public void setDescription(String description) {
        this._description = description;
    }
    public String getName() {
        return _name;
    }
    public void setName(String name) {
        this._name = name;
    }
    public Command[] getCommands() {

        return _command;

    }



    public void setCommands(Command[] command) {

        this._command = command;

    }

    private String _name;
    private String _description;
    private Command[] _command;


}
