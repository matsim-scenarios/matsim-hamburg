#!/bin/bash --login
#$ -l h_rt=792000
#$ -N
#$ -o ./logfile_$JOB_NAME.log
#$ -j y
#$ -m be
#$ -M
#$ -cwd
#$ -pe mp 12
#$ -l mem_free=12G

date
hostname

# configure matsim version
classpath="matsim-hamburg-1.0-SNAPSHOT/matsim-hamburg-1.0-SNAPSHOT.jar"

echo "***"
echo "classpath: $classpath"
echo "***"

# java command
java_command="java -Djava.awt.headless=true -Xmx120G -cp $classpath"

# main
main="org.matsim.run.RunHamburgScenario"

# arguments
arguments="scenarios/input/$JOB_NAME.config.xml --iterations 10 --runId $JOB_NAME"

# command
command="$java_command $main $arguments"

echo ""
echo "command is $command"

echo ""
echo "using alternative java"
module add java/11
java -version

$command
