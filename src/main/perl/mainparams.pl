#!/usr/bin/perl -w

use strict;
use warnings;

open(MYFILE, 'mainparams');

while(<MYFILE>) {
	chomp;
	
	if($_ =~ m/^\/\/(.*)/i) {
		chomp;
		my $test = $1;
		$test =~ s/^\s+//;
		$test = lc($test);
		$test = ucfirst($test);
		print "<f:block><strong>$test</strong></f:block>\n\n";
	}
	
	if($_ =~ m/private final Boolean (.*);\s?\/\/\s?(\([a-zA-Z]\))?\s?(.*)/i) {
		my $variable = $1;
		my $description = $3;
		$description =~ s/^\s+//;
		$description =~ s/\s+$//;
		$description = ucfirst($description);
		print "<f:entry title=\"\${%%$description}\" help=\"\${rootURL}/../plugin/structure/help-$variable.html\">\n";
        print "\t<f:checkbox name=\"extraParams.$variable\" value=\"\${instance.extraParams.$variable}\" checked=\"\${instance.extraParams.$variable}\" />\n";
        print "</f:entry>\n\n";
	}
	
	if($_ =~ m/private final Double (.*);\s?\/\/\s?(\([a-zA-Z]\))?\s?(.*)/i) {
        my $variable = $1;
        my $description = $3;
        $description =~ s/^\s+//;
        $description =~ s/\s+$//;
        $description = ucfirst($description);
        print "<f:entry title=\"\${%%$description}\" help=\"\${rootURL}/../plugin/structure/help-$variable.html\">\n";
        print "\t<f:textbox name=\"extraParams.$variable\" value=\"\${instance.extraParams.$variable}\" />\n";
        print "</f:entry>\n\n";
    }
    
    if($_ =~ m/private final Long (.*);\s?\/\/\s?(\([a-zA-Z]\))?\s?(.*)/i) {
        my $variable = $1;
        my $description = $3;
        $description =~ s/^\s+//;
        $description =~ s/\s+$//;
        $description = ucfirst($description);
        print "<f:entry title=\"\${%%$description}\" help=\"\${rootURL}/../plugin/structure/help-$variable.html\">\n";
        print "\t<f:textbox name=\"extraParams.$variable\" value=\"\${instance.extraParams.$variable}\" />\n";
        print "</f:entry>\n\n";
    }
}
close(MYFILE);