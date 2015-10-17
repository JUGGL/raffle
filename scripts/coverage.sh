#!/bin/sh
#
# Copyright 2015 Joseph "Deven" Phillips
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#

function coverageFilePath { echo "target/site/cobertura/coverage.xml"; }

PROJECT_TOKEN=${CODACY_PROJECT_TOKEN}

if [ -z "${PROJECT_TOKEN}" ]; then
    echo "Project token not found."
    exit 1
fi

COMMIT_UUID=`git rev-parse --verify HEAD`

if [[ $? != 0 ]]; then
    echo "Not a valid git repository."
    exit 1
fi

if [ -f `coverageFilePath "2.11"` ]; then
    COVERAGE_REPORT=`coverageFilePath "2.11"`
else
    if [ -f `coverageFilePath "2.10"` ]; then
        COVERAGE_REPORT=`coverageFilePath "2.10"`
    else
        echo "Coverage report not found."
        exit 1
    fi
fi

COVERAGE_REPORT_CONTENT=`cat ${COVERAGE_REPORT}`

curl -X POST https://api.codacy.com/2.0/coverage/${COMMIT_UUID}/scala -d ${COVERAGE_REPORT_CONTENT} \
--header "Content-Type: application/json" \
--header "project_token: ${PROJECT_TOKEN}"