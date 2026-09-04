#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
${symbol_pound}!/bin/sh

java -classpath "libs/*:openl/lib/*" ${package}.Main 10
