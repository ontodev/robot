#!/bin/sh

## Check for Cygwin, use grep for a case-insensitive search
IS_CYGWIN="FALSE"
if uname | grep -iq cygwin; then
    IS_CYGWIN="TRUE"
fi

# Variable to hold path to this script
# Start by assuming it was the path invoked.
ROBOT_SCRIPT="$0"

# Handle resolving symlinks to this script.
# Using ls instead of readlink, because bsd and gnu flavors
# have different behavior.
while [ -h "$ROBOT_SCRIPT" ] ; do
  ls=`ls -ld "$ROBOT_SCRIPT"`
  # Drop everything prior to ->
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    ROBOT_SCRIPT="$link"
  else
    ROBOT_SCRIPT=`dirname "$ROBOT_SCRIPT"`/"$link"
  fi
done

# Directory that contains the this script
DIR=$(dirname "$ROBOT_SCRIPT")

if [ $IS_CYGWIN = "TRUE" ]
then
    exec java $ROBOT_JAVA_ARGS -jar "$(cygpath -w $DIR/robot.jar)" "$@"
else
    exec java $ROBOT_JAVA_ARGS -jar "$DIR/robot.jar" "$@"
fi
