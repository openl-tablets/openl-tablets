##### Specifying Data in Data Tables with List and Map Fields

A **list** represents an ordered sequence of objects. Unlike array, a list can contain elements of any type.
A **map** is a collection of key-value pairs. Each element of the map always has two values, a key and a value.

To define data table for lists and maps, use the following syntax:

-   for lists, [i]:\<element datatype\>
    
    [i] is order number
    
-   for maps, [“key”]:\<element datatype\>

If a datatype table field is a list or a map, use the following syntax:

-   for lists, \<attribute name\>[i]:\<element datatype\>
-   for maps, \<attribute name\>[“key”]:\<element datatype\>

An example of the data table with a list of values used for zip codes is as follows:

![](../../../../ref_guide_images/7458aefdfc4db9a8041a3ae134e6e3b4.png)

*Data table using a list field defined in the datatype table*

Values of the list type can also be defined as a comma-separated list.

An example of the datatype table for this data table is as follows:

![](../../../../ref_guide_images/09231cabcd3db615454ccae54448f258.png)

*Datatype table with a list field*

An example of the data table with a map of values used for zip codes is as follows:

![](../../../../ref_guide_images/df2fdf6ea29aceddcd420c67e2f392d5.png)

*Data table for the Map data type containing an aggregated object*

An example of the datatype table for this table is as follows:

![](../../../../ref_guide_images/cb2be397a3418304d66cdd351649fd52.png)

*A datatype table for the address custom data type*

