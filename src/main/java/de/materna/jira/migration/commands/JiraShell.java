package de.materna.jira.migration.commands;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.FieldType;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@ShellComponent 
@Log4j2
public class JiraShell {    
    
    private boolean isConnected = false;
    private JiraRestClient jiraClient;

    @ShellMethod(value = "Logout from a jira instance.")    
    public void disconnect() throws IOException {
        isConnected = false;
        this.jiraClient.close();
    }

    @ShellMethod(value = "Login to a jira instance.")    
    public void connect(String remoteServer, String username, String password) {
        System.out.println("Logging in: " + remoteServer);
        
        try {
            JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
            jiraClient = factory.createWithBasicHttpAuthentication(URI.create(remoteServer), username, password);
            this.isConnected = true;
        } catch (Exception e) {
            System.err.println("ERROR" + e.getMessage());
            log.error("Could not connect to JIRA:", e);
        }
    }    

    @ShellMethod(value = "export custom fields from jira")    
    public void importFields(String fileName) {    
        System.out.println("Importing custom fields to: " + this.jiraClient.getSessionClient().getCurrentSession().claim().getUserUri());
        
        ObjectMapper mapper = new ObjectMapper();

        try {
            ArrayList<Field> readValue = mapper.readValue(new File(fileName), new TypeReference<ArrayList<Field>>() {});

            for(Field field : readValue) {
            }

        } catch (IOException e) {
            System.err.println("ERROR" + e.getMessage());
            log.error("Could not import fields:", e);
        }

    }

    @ShellMethod(value = "export custom fields from jira")    
    public void exportFields(String fileName) {    
        System.out.println("Exporting custom fields from: " + this.jiraClient.getSessionClient().getCurrentSession().claim().getUserUri());
        final Iterable<Field> fields = jiraClient.getMetadataClient().getFields().claim();

        List<Field> exportFields = new ArrayList<>();
        for (Field field : fields) {

            if(field.getFieldType() == FieldType.CUSTOM) {
                log.info("Field name: " + field.getName() + ", id: " + field.getId());
                exportFields.add(field);
            } 
        }
        
        ObjectMapper mapper = new ObjectMapper();

        // Write the object as JSON to a file
        try {
            mapper.writeValue(new File(fileName), exportFields);
        } catch (IOException e) {
            System.err.println("ERROR" + e.getMessage());
            log.error("Could not export fields:", e);
        }
	} 

    public Availability importFieldsAvailability() {
        return exportFieldsAvailability();
    }

    public Availability exportFieldsAvailability() {
        return isConnected
            ? Availability.available()
            : Availability.unavailable("you are not connected to jira");
    }
}  
