# ScheduleCheck
AKA: AspenCheck, AspenInfo, X2Info

ScheduleCheck is a program that gets information (such as schedule information) from Aspen. It is currently used as the backend for the Melrose High School's [AspenDash](https://aspen.studenttech.tk).

Any district using Aspen can use several features of ScheduleCheck without needing a specialized configuration, as described below.

## Endpoints:
All endpoints are prefixed with '/api/v1/{district-id}' where `district-id` is the ID of your Aspen instance; all endpoints return a JSON object. Pass `ASPEN_UNAME` and `ASPEN_PASS` as header to get personalized results.
- If the URL of your Aspen instance is not in the format of `https://{district-id}.myfollet.com/aspen` then `config.json` needs to be updated to reflect it. Additionally, if you want other configuration information (i.e. grade scale), make an issue or a pull request with the necessary config to add to `config.json`

* `/aspen`: All endpoints involving aspen
  * [`/schedule`](#schedule): Returns schedule information, such as day, block, etc. (config / auth optional)
  * `/student`: Returns grade, ID, etc regarding the student (auth required)
  * [`/course`](#course-list): Index of enrolled courses, their grades, teachers, etc (auth required)
    * Can use `moreData=true` to get additional info about courses including a list of their assignments
    * Can use `term={number}` to change `currentTermGrade` and the term of the assignments is `moreData=true` is specified
  * [`/course/{course-id}`](#course): Same info as `/course?moreData=true` but for only one course (can use `term`)
  * [`/course/{course-id}/assignment`](#assignment-list): Gets a list of all of a course's assignments and their grades

* `/announcements`: Returns all announcements scheduled to run (sources gathered from config)

## API Examples

### All
```javascript
"asOf": 1589911102,
"data": {
    ...
},
"errors": {
    "title": null,
    "id": 0,
    "details": null
}
```

### Schedule
https://aspencheck.herokuapp.com/api/v1/{district}/aspen/schedule
```shell script
curl https://aspencheck.herokuapp.com/api/v1/{district}/aspen/schedule | json_pp
```

```javascript
"data": {
    "day": 2,
    "classInSession": true,
    "block": "E",
    "advisoryBlock": "Z",
    "blockOfDay": 6,
    "blockOrder": ["A" ,"D" ,"B" ,"F" ,"G" ,"E"],
    "dayBlockOrder": {
        "1": ["A", "B", "C", "D", "E", "F"],
        "2": ["A", "D", "B", "F", "G", "E"],
        "3": ["A", "B", "C", "E", "D", "G"],
        "4": ["A", "C", "D", "G", "E", "F"],
        "5": ["A", "B", "C", "F", "G", "D"],
        "6": ["A", "C", "B", "G", "F", "E"],
        "7": ["B", "C", "D", "E", "F", "G"]
    }
}
```

### Course List
https://aspencheck.herokuapp.com/api/v1/{district}/aspen/course
```shell script
curl -H "ASPEN_UNAME: {username}" -H 'ASPEN_PASS: {password}' -X GET 'https://aspencheck.herokuapp.com/api/v1/{district}/aspen/course' | json_pp
```
```javascript
"data": [
    {
        "id" : "ABC00000123Wnt",
        "name" : "Class Name",
        "teacher" : "Last, First",
        "currentTermGrade" : "94.3 A",
        "room" : "451",
        "code" : "ABC-123",
        "term" : "FY",

        "postedGrades" : {
             "4" : "",
             "2" : "A",
             "3" : "A",
             "1" : "A"
         },
        "assignments" : [
               {...},
        ],
        "categoryTable" : {
            "1" : [
                {
                    "name" : "Assessments",
                    "weight" : "50.0",
                    "average" : "91.4",
                    "letter" : "A-"
                },
                ...
            ],
            "2" : [...],
            ...
        }
    },
    ...
]
```
**Notes**
- `postedGrades`, `assignments`, and `categoryTable` are only available when moreData=true

### Course
https://aspencheck.herokuapp.com/api/v1/{district}/aspen/course/{course-id}
```shell script
curl -H "ASPEN_UNAME: {username}" -H 'ASPEN_PASS: {password}' -X GET 'https://aspencheck.herokuapp.com/api/v1/{district}/aspen/course/{course-id}' | json_pp
```
```javascript
"data": {
    "id" : "ABC00000123Wnt",
    "name" : "Class Name",
    "teacher" : "Last, First",
    "currentTermGrade" : "94.3 A",
    "room" : "451",
    "code" : "ABC-123",
    "term" : "FY",

    "postedGrades" : {
        "1" : "A",
        "2" : "B",
        "3" : "C",
        "4" : "D"
    },
    "assignments" : [
           {...},
    ],
    "categoryTable" : {
        "1" : [
            {
                "name" : "Assessments",
                "weight" : "50.0",
                "average" : "91.4",
                "letter" : "A-"
            },
            ...
        ],
        "2" : [...],
        ...
    }
}
```
**Notes**
- `postedGrades`, `assignments`, and `categoryTable` are only available when moreData=true

### Assignment List
https://aspencheck.herokuapp.com/api/v1/{district}/aspen/course/{course-id}/assignment
```shell script
curl -H "ASPEN_UNAME: {username}" -H 'ASPEN_PASS: {password}' -X GET 'https://aspencheck.herokuapp.com/api/v1/{district}/aspen/course/{course-id}/assignment' | json_pp
```
```javascript
"data": [
    {
        "id": "ABC00000123Gtk",
        "name": "Assignment Name",
        "credit" : "100%",
        "score" : "4.5",
        "possibleScore" : "5.0",
        "letterGrade": "A-",
        "dateDue": "M/D/YYYY",
        "dateAssigned": "M/D/YYYY"
    },
    ...
]
```
**Notes**
- `credit` can also be "Ungraded" or "Msg calculates ..."
- `score` is null if no grade or missing
- `possibleScore` is null if no grade, if missing
- `letterGrade` is null if no grading scale provided
