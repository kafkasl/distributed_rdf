package main;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;

import java.io.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.*;

public class ConverterXmlToSQL {

    // JDBC driver name and database URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String URL_ADD = "?useServerPrepStmts=false&rewriteBatchedStatements=true";

    static final String DATABASE_NAME = "XMLtoSQL";
    
    static final int BATCH_SIZE = 2500;
    static final double INSERT_LIMIT = Double.POSITIVE_INFINITY;
    //static final double INSERT_LIMIT = 300;


    private static Connection connectToDB(String url, String user, String password) {

        try{
            Class.forName("com.mysql.jdbc.Driver");
        }
        catch(Exception e){
            System.out.println("Could not find Class com.mysql.jdbc.Driver");
            System.exit(0);
        }

        System.out.println("Connecting to database");
        Connection conn = null;

        try{
            conn = DriverManager.getConnection(url + DATABASE_NAME + URL_ADD, user, password);
        }
        catch(SQLException e1){
            try{
                conn = DriverManager.getConnection(url + URL_ADD, user, password);
                createDB(conn, DATABASE_NAME);
                conn.close();
                conn = DriverManager.getConnection(url + "/" + DATABASE_NAME + URL_ADD, user, password);
            }
            catch(SQLException e2){
                System.out.println("Could not create the database " + DATABASE_NAME);
                System.exit(0);
            }
        }
        return conn;
    }

    private static void createDB(Connection conn, String name) {
        System.out.println("Creating database");
        Statement stmt = null;

        try {
            stmt = conn.createStatement();
            String sql = "CREATE DATABASE " + name;
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("Database already exists");
        }
    }

    private static void createOrCleanTable(Connection conn, String tableName, ArrayList<String> columns) {
        if(columns.size() ==0){
            System.out.println("No columns in table!");
        }

        Statement stmt = null;
        Boolean cleanTable = false;
        String createSQLTable = "CREATE TABLE " + tableName + " (" + columns.get(0).replace(" ", "_") + " VARCHAR(255)";
        for(int i = 1; i < columns.size(); ++i){
            createSQLTable += ("," + columns.get(i).replace(" ", "_") + " VARCHAR(255)");
        }
        createSQLTable += ")";
        try {
            stmt = conn.createStatement();
        }
        catch(SQLException e){
            System.out.println("Could not create the statement");
        }
        try{
            stmt.executeUpdate(createSQLTable);
        } catch (SQLException e) {
            System.out.println("Table already exists. Proceeding to the cleaning.");
            cleanTable = true;
        }
        if(cleanTable){
            try{
                System.out.println("Cleaning table");
                String sql = "DROP TABLE " + tableName;
                stmt.executeUpdate(sql);
                stmt.executeUpdate(createSQLTable);
                stmt.close();
            }
            catch(SQLException e){
                System.out.println("Could not remove and create the table");
            }
        }
    }
    
    private static String normalizeInput(String input){
        ArrayList<String> names = new ArrayList<String>(Arrays.asList(",", "'", "\\[", "\\]", "\\(", "\\)"));
        input = input.replaceAll(" ", "_");
        for(int i = 0; i < names.size(); ++i){
            input = input.replaceAll(names.get(i), "");
        }
        return input;
    }
    
    private static void insertValues(Connection conn, String tableName, ArrayList<ArrayList<String>> valuesArray){
        if(valuesArray.size() == 0){
            System.out.println("No values to insert");
        }
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
        }
        catch(SQLException e){
            System.out.println("Could not create the statement");
        }
        for(ArrayList<String> values: valuesArray){
            if(values.size() == 0){
                System.out.println("Empty row");
            }
            else{
                String insertSQLRow = "INSERT INTO " + tableName + " VALUES ('" + normalizeInput(values.get(0)) + "'";
                for(int i = 1; i < values.size(); ++i){
                    insertSQLRow += ", '" + normalizeInput(values.get(i)) + "'";
                }
                insertSQLRow += ")";       
                try {
                    stmt.addBatch(insertSQLRow);
                } catch (SQLException e) {
                    System.out.println("Could not add batch to statement");
                }
            }
        }
        try{
            stmt.executeBatch();
            stmt.close();
        }
        catch(SQLException e){
            e.printStackTrace();
            System.out.println("Could not insert the values");
        }
    }
    
    private static void disableDatabaseConstraints(Connection conn, String tableName) {
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("ALTER TABLE " + tableName + " DISABLE KEYS");
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Could not disable keys and constraints");
        }
    }

    private static void enableDatabaseConstraints(Connection conn, String tableName) {
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("ALTER TABLE " + tableName + " ENABLE KEYS");
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Could not enable keys and constraints");
        }
    }

    private static void Convert(String pathToXML, Connection conn) {

        File inputFile = new File(pathToXML);
        String[] tableNameList = pathToXML.split("/");
        String name = ((tableNameList[tableNameList.length - 1]).split("\\."))[0];

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try{
            dBuilder = dbFactory.newDocumentBuilder();
        }
        catch(ParserConfigurationException e){
            System.out.println("Error loading input file");
        }
        Document doc = null;
        try{
            doc = dBuilder.parse(inputFile);
        }
        catch(Exception e){
            System.out.println("Error parsing input file");
        }
        doc.getDocumentElement().normalize();

        NodeList nList = doc.getElementsByTagName("record");
        ArrayList<String> columns = new ArrayList<String>();
        ArrayList<ArrayList<String>> values = new ArrayList<ArrayList<String>>();
        Boolean createTable = true;
        disableDatabaseConstraints(conn, name);
        for (int i = 0; i < nList.getLength() && i < INSERT_LIMIT; i++) {
            Node nNode = nList.item(i);
            NodeList elemList = nNode.getChildNodes();
            ArrayList<String> currentRow = new ArrayList<String>();
            for (int j = 0; j < elemList.getLength(); ++j) {
                if (elemList.item(j).getNodeName() == "field") {
                    if (createTable){
                        columns.add(((Element) elemList.item(j)).getAttribute("name"));
                    }
                    currentRow.add(elemList.item(j).getTextContent());
                }
            }
            if (i == 0){
                createOrCleanTable(conn, name, columns);
                createTable = false;
            }
            if ((i % BATCH_SIZE) == 0 && i > 0){
                values.add(currentRow);
                insertValues(conn, name, values);
                values.clear();
                System.out.println(i + "/" + nList.getLength());
            }
            else{
                values.add(currentRow);
            }
        }
        System.out.println(nList.getLength() + "/" + nList.getLength());
        insertValues(conn, name, values);
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("ALTER TABLE " + name + " ADD prkey INT NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY(prkey)");
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Could not add primary key");
        }
        enableDatabaseConstraints(conn, name);
    }
    
    public static void main(String[] args) {

        Connection conn = null;

        String xmlPath = args[0];
        String url = args[1];
        String user = args[2];
        String pass = args[3];

        conn = connectToDB(url, user, pass);
        Convert(xmlPath, conn);
    }
}
