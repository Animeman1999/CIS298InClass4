package edu.kvcc.cis298.inclass3.inclass3;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import edu.kvcc.cis298.inclass3.inclass3.database.CrimeBaseHelper;
import edu.kvcc.cis298.inclass3.inclass3.database.CrimeCursorWrapper;
import edu.kvcc.cis298.inclass3.inclass3.database.CrimeDbSchema;
import edu.kvcc.cis298.inclass3.inclass3.database.CrimeDbSchema.CrimeTable;

/**
 * Created by dbarnes on 10/28/2015.
 */
public class CrimeLab {

    //Static variable to hold the instance of CrimeLab
    //Rather than returning a new instance of CrimeLab,
    //we will return this variable that holds our instance.
    private static CrimeLab sCrimeLab;

    //Private variable for the context
    private Context mContext;

    //private variable for the database that this crimelab will use
    private SQLiteDatabase mDatabase;

    //This is the method that will be used to get an instance of
    //CrimeLab. It will check to see if the current instance in the
    //variable is null, and if it is, it will create a new one using
    //the private constuctor. If it is NOT null, it will just return
    //the instance that exists.
    public static CrimeLab get(Context context) {
        if (sCrimeLab == null) {
            sCrimeLab = new CrimeLab(context);
        }
        return sCrimeLab;
    }

    //This is the constuctor. It is private rather than public.
    //It is private because we don't want people to be able to
    //create a new instance from outside classes. If they want
    //to make an instance, we want them to use the get method
    //declared right above here.
    private CrimeLab(Context context) {

        //Assign the passed in context a class level one.
        mContext = context.getApplicationContext();

        //Use the context in conjunction with the CrimeBaseHelper
        //class that we wrote to get the writeable database.
        //We didn't write the getWritableDatabase function that
        //is being called. It came from the parent class that
        //CrimeBaseHelper inherits from.
        mDatabase = new CrimeBaseHelper(mContext)
                .getWritableDatabase();

    }

    //Method to add a new crime to the database. This method will get
    //called when a user clicks on the add button of the toolbar.
    public void addCrime(Crime c) {
        //Get the content values that we would like to stick into the
        //database by sending it the crime that needs to be translated
        ContentValues values = getContentValues(c);

        //Call the insert method of our class level version of the CrimeBaseHelper
        //class. We did not write the insert method. It came from the parent class
        //of CrimeBaseHelper. We are just using it.
        mDatabase.insert(CrimeTable.NAME, null, values);
    }

    public void updateCrime(Crime crime) {
        //Get the UUID out of the crime as a string. This will be used
        //in the WHERE clause of the SQL to find the row we want to update.
        String uuidString = crime.getId().toString();
        //Create the content values that will be used to do
        //the update of the model
        ContentValues values = getContentValues(crime);

        //Update a specific crime with the values from the content values
        //for a crime that has the UUID of the one in uuidString.
        //
        //The update method has the following signature:
        //First Param: Table Name
        //Second Param: Values to update
        //Third Param: WHERE clause to know which row to update.
        //Fourth Param: String array of parameters to bind to the ?'s
        //in the WHERE clause.
        //
        //The finished SQL will look something like:
        //UPDATE Crimes WHERE uuid = ? SET (param1, param2, ...)
        //  VALUES (value1, value2, ...);
        //Where the ? will become the value in uuidstring
        mDatabase.update(CrimeTable.NAME, values,
            CrimeTable.Cols.UUID + " = ?",
            new String[] { uuidString });
    }

    //Getter to get the crimes
    public List<Crime> getCrimes() {
        //Create a List to hold all of the crimes
        List<Crime> crimes = new ArrayList<>();

        //Create a new crimeCursorWrapper. It will be returned from
        //the call to queryCrimes, which is the method written at the
        //bottom of this class. We pass in null for both parameters
        //because we do not want a Where clause, nor where parameters
        CrimeCursorWrapper cursor = queryCrimes(null, null);

        try {
            //Move to the first entry in the data result
            cursor.moveToFirst();
            //while there is another entry in the data result
            while (!cursor.isAfterLast()) {
                //Call the getCrime method which will do the work
                //of taking the data from the 'read' row and turn
                //it into a new crime. The returned crime gets added
                //to the list.
                crimes.add(cursor.getCrime());
                //Move to the next row in the result set
                cursor.moveToNext();
            }
        } finally {
            //close the cursor
            cursor.close();
        }
        //return the crimes list
        return crimes;
    }

    //Method to get a specific crime based on the
    //UUID that is passed in.
    public Crime getCrime(UUID id) {

        //Create a new crimecursorwrapper. This time we are passing
        //in a where clause, and a string array that is the where
        //arguments. This will narrow our query down to a single
        //entry with the passed in UUID.
        CrimeCursorWrapper cursor = queryCrimes(
                CrimeTable.Cols.UUID + " = ?",
                new String[] {id.toString()}
        );

        try {
            //If we did not get a result
            if (cursor.getCount() == 0) {
                //return null. Didn't find it.
                return null;
            }

            //Move to the first entry in the result set
            cursor.moveToFirst();
            //return the result changed over to a Crime model
            return cursor.getCrime();
        } finally {
            //close the cursor
            cursor.close();
        }

    }

    //static method to do the work of taking in a crime and creating
    //a contentValues object that can be used to insert the crime into
    //the database. The ContentValues class operates as a hash table, or
    //"key => value" array. The key refers to the column name of the database
    //and the value refers to the value we would like to put into the database.
    private static ContentValues getContentValues(Crime crime) {
        //Make a new ContentValues object
        ContentValues values = new ContentValues();
        //Put the UUID converted to a string
        values.put(CrimeTable.Cols.UUID, crime.getId().toString());
        //Put the title. No conversion is neccessary since it is a string
        values.put(CrimeTable.Cols.TITLE, crime.getTitle());
        //Put the Date. Note that it has to be changed from a Date object
        //to a Timestamp. That's why we are calling the getTime method at the
        //end. The database can only store the Date as a Timestamp
        values.put(CrimeTable.Cols.DATE, crime.getDate().getTime());
        //Welcome to the ternary operator. It evaluates an expression
        //to true or false. If it is true, the value between the ? and :
        //is used. If it is false, the value after the : is used. This
        //is like a one line if else statement. We are using it here
        //change the true / false value into a 0 or 1 since that is how
        //the database will store a boolean.

        //Here is the long version:

        //int intToUse;
        //if (crime.isSolved) {
        //    intToUse = 1;
        //} else {
        //    intToUse = 0;
        //}
        //values.put(CrimTable.Cols.SOLVED, intToUse);
        values.put(CrimeTable.Cols.SOLVED, crime.isSolved() ? 1 : 0);

        return values;
    }

    //Method to query the crimes table for crimes. It takes in a where
    //clause and where args that can be used for the query. It will return
    //a result set that we can look through to see the returned crimes.
    private CrimeCursorWrapper queryCrimes(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(
            CrimeTable.NAME, //Table name
                null, //Columns. Null means all of them
                whereClause, //where
                whereArgs, //args for where
                null, //groupby
                null, //having
                null //orderby
        );
        return new CrimeCursorWrapper(cursor);
    }
}
