#!/bin/bash

set -e


# Checks out all steps from scratch in new folders:

rm -rf step*

check_step() {
	echo "Checking out step$1"
	STEP=$1
	DIFF=$((STEP-1))
	# git checkout list-step$1 --quiet
	git checkout list-steps~$DIFF --quiet
	git checkout . --quiet
	git rev-list --format=%B --max-count=1 HEAD
	mkdir -p step$1
	#sleep .1
	mv project step$1
	mv src step$1
	mv list-deep step$1 2> /dev/null || true # that directory may not be present, eg in step1
}

check_step 1
check_step 2
check_step 3
check_step 4
check_step 5


# Saves the newly-generated steps to updates the ones in master without conflict:

git checkout . &> /dev/null

git add step*
git stash --quiet
git checkout master --quiet
rm -rf step*
git add step*
git stash pop


