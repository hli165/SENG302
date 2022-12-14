/**
 * Returns true if the key pressed is a valid key for dates, otherwise false.
 * Valid keys are numbers, letters, forward slashes, backspace, enter, and delete.
 * @param event key press event
 * @returns {boolean} true if the key pressed is a valid key for dates, otherwise false
 */
function checkDateKeys(event) {
    return '1234567890/'.includes(event.key) || [8, 13, 46].includes(event.keyCode) || 65 <= event.keyCode && event.keyCode <= 90;
}

function checkTimeKeys(event) {
    return '1234567890: apm'.includes(event.key) || [8, 13, 46].includes(event.keyCode);
}

/**
 * Returns true if a modal is open, otherwise false.
 * @returns {boolean} true if a modal is open, otherwise false
 */
function isModalOpen() {
    return document.getElementsByClassName('modal fade show').length > 0
}

/**
 * Returns the modal that is currently open
 * @returns {Element} modal that is currently open
 */
function getOpenModal() {
    return document.getElementsByClassName('modal fade show')[0]
}

/**
 * Customises the sprint modal attributes with depending on what sprint it should display and whether it's being used
 * for adding or editing a sprint.
 */
function sprintModalSetup() {
    const sprintModal = document.getElementById('sprintModal')
    sprintModal.addEventListener('show.bs.modal', function (event) {
        // Button that triggered the modal
        const button = event.relatedTarget

        // Extract info from data-bs-* attributes
        const sprint = JSON.parse(button.getAttribute('data-bs-sprint'))
        const type = button.getAttribute('data-bs-type')

        // Update the modal's content.
        const modalTitle = sprintModal.querySelector('.modal-title')
        const modalBodyInputs = sprintModal.querySelectorAll('.modal-body input')
        const modalBodyTextArea = sprintModal.querySelector('.modal-body textarea')
        const modalButton = sprintModal.querySelector('.modal-footer button')
        const modalForm = sprintModal.querySelector('form')

        if (type === 'add') {
            modalTitle.innerText = 'Add Sprint'
            modalButton.innerHTML = 'Add Sprint'
            modalForm.action = 'add-sprint'
            modalForm.setAttribute('data-sprint-id', -1)
        } else {
            modalTitle.innerText = 'Edit Sprint'
            modalButton.innerHTML = 'Save Sprint'
            modalForm.action = `edit-sprint/${sprint.id}`
            modalForm.setAttribute('data-sprint-id', sprint.id)
        }

        modalBodyInputs[0].value = sprint.name
        modalBodyTextArea.value = sprint.description

        sprintStartDatePicker.dates.setValue(tempusDominus.DateTime.convert(new Date(sprint.startDate)));
        sprintStartDatePicker.updateOptions({restrictions: {minDate: new Date(projectStartDate), maxDate: new Date(projectEndDate)}});
        sprintEndDatePicker.dates.setValue(tempusDominus.DateTime.convert(new Date(sprint.endDate)));
        sprintEndDatePicker.updateOptions({restrictions: {minDate: new Date(projectStartDate), maxDate: new Date(projectEndDate)}});

        // Initial run of updateSprintDateError function in case initial values are invalid
        updateSprintDateError();
        updateCharsLeft('sprintName', 'sprintNameLength', 50);
        updateCharsLeft('sprintDescription', 'sprintDescriptionLength', 500);
    })
}

/**
 * Customises the delete modal attributes with depending on what type of thing (e.g. sprint, event, milestone) is
 * being deleted, and which thing it is.
 */
function deleteModalSetup() {
    const deleteModal = document.getElementById('deleteModal')
    deleteModal.addEventListener('show.bs.modal', function (event) {
        // Button that triggered the modal
        const button = event.relatedTarget

        // Extract info from data-bs-* attributes
        const id = button.getAttribute('data-bs-id')
        const name = button.getAttribute('data-bs-name')
        const type = button.getAttribute('data-bs-type')
        const members = button.getAttribute('data-bs-member')

        // Update the modal's content.
        const modalTitle = deleteModal.querySelector('.modal-title')
        const modalBodyLabel = deleteModal.querySelector('.modal-body label')
        const modalButton = deleteModal.querySelector('.modal-footer button')
        const modalLink = deleteModal.querySelector('.modal-footer a')
        if (members) {
            modalBodyLabel.textContent = `WARNING: This group contains ${members} member(s), deleting this group will` +
                ` remove all users from this group and cannot be undone. Are you sure you want to delete ${name}?`
        } else {
            modalBodyLabel.textContent = `Are you sure you want to delete ${name}?`
        }
        modalTitle.innerText = `Delete ${type.charAt(0).toUpperCase() + type.slice(1)}`
        modalButton.innerHTML = `Delete ${type.charAt(0).toUpperCase() + type.slice(1)}`
        modalLink.href = `delete-${type}/${id}`
    })
}

/**
 * Customises the project modal attributes with depending on what project it should display and whether it's being used
 * for adding or editing a project.
 */
function projectModalSetup() {
    const projectModal = document.getElementById('projectModal')
    projectModal.addEventListener('show.bs.modal', function (event) {
        // Button that triggered the modal
        const button = event.relatedTarget

        // Extract info from data-bs-* attributes
        const project = JSON.parse(button.getAttribute('data-bs-project'))

        // Update the modal's content.
        const modalBodyInputs = projectModal.querySelectorAll('.modal-body input')
        const modalBodyTextArea = projectModal.querySelector('.modal-body textarea')

        modalBodyInputs[0].value = project.name
        modalBodyTextArea.value = project.description

        projectStartDatePicker.dates.setValue(tempusDominus.DateTime.convert(new Date(project.startDate)));
        projectEndDatePicker.dates.setValue(tempusDominus.DateTime.convert(new Date(project.endDate)));

        // Initial run of updateProjectDateError function in case initial values are invalid
        updateProjectDateError();
        updateCharsLeft('projectName', 'projectNameLength', 50);
        updateCharsLeft('projectDescription', 'projectDescriptionLength', 500);
    })
}

/**
 * Customises the milestone modal attributes with depending on what milestone it should display and whether it's being
 * used for adding or editing a milestone.
 */
function milestoneModalSetup() {
    const milestoneModal = document.getElementById('milestoneModal')
    milestoneModal.addEventListener('show.bs.modal', function (event) {
        // Button that triggered the modal
        const button = event.relatedTarget

        // Extract info from data-bs-* attributes
        const milestone = JSON.parse(button.getAttribute('data-bs-milestone'))
        const type = button.getAttribute('data-bs-type')

        // Update the modal's content.
        const modalTitle = milestoneModal.querySelector('.modal-title')
        const modalBodyInput = milestoneModal.querySelector('.modal-body input')
        const modalButton = milestoneModal.querySelector('.modal-footer button')
        const modalForm = milestoneModal.querySelector('form')

        if (type === 'add') {
            modalTitle.innerText = 'Add Milestone'
            modalButton.innerHTML = 'Add Milestone'
            modalForm.action = 'add-milestone'
        } else {
            modalTitle.innerText = 'Edit Milestone'
            modalButton.innerHTML = 'Save Milestone'
            modalForm.action = `edit-milestone/${milestone.id}`
            modalForm.setAttribute('milestone', milestone)
        }

        modalForm.setAttribute('object', milestone);
        modalBodyInput.value = milestone.milestoneName

        milestoneDatePicker.dates.setValue(tempusDominus.DateTime.convert(new Date(milestone.milestoneDate)));
        // Set minimum and maximum dates for greying out in the calendar
        milestoneDatePicker.updateOptions({restrictions: {minDate: new Date(projectStartDate), maxDate: new Date(projectEndDate)}});

        // Initial run of validation functions in case initial values are invalid
        validateModalDate('milestoneDate', 'milestoneModalButton', 'milestoneDateAlertBanner', 'milestoneDateAlertMessage')
        updateCharsLeft('milestoneName', 'milestoneNameLength', 30)
        $('#' + modalButton.getAttribute("id")).prop('hidden', false);
    })
}

/**
 * Customises the deadline modal attributes with depending on what deadline it should display and whether it's being
 * used for adding or editing a deadline.
 */
function deadlineModalSetup() {
    const deadlineModal = document.getElementById('deadlineModal')
    deadlineModal.addEventListener('show.bs.modal', function (event) {
        // Button that triggered the modal
        const button = event.relatedTarget

        // Extract info from data-bs-* attributes
        const deadline = JSON.parse(button.getAttribute('data-bs-deadline'))
        const type = button.getAttribute('data-bs-type')

        // Update the modal's content.
        const modalTitle = deadlineModal.querySelector('.modal-title')
        const modalBodyInputs = deadlineModal.querySelectorAll('.modal-body input')
        const modalButton = deadlineModal.querySelector('.modal-footer button')
        const modalForm = deadlineModal.querySelector('form')

        if (type === 'add') {
            modalTitle.innerText = 'Add Deadline'
            modalButton.innerHTML = 'Add Deadline'
            modalForm.action = 'add-deadline'
        } else {
            modalTitle.innerText = 'Edit Deadline'
            modalButton.innerHTML = 'Save Deadline'
            modalForm.action = `edit-deadline/${deadline.id}`
            modalForm.setAttribute('deadline', deadline)
        }

        modalForm.setAttribute('object', deadline);
        modalBodyInputs[0].value = deadline.deadlineName;
        deadlineDatePicker.dates.setValue(tempusDominus.DateTime.convert(new Date(deadline.deadlineDate)));
        // Set min and max dates based on project dates
        deadlineDatePicker.updateOptions({restrictions: {minDate: new Date(projectStartDate), maxDate: new Date(projectEndDate)}});


        // Initial run of validation functions in case initial values are invalid
        validateModalDateTime('deadlineDate', 'deadlineModalButton', 'deadlineDateAlertBanner', 'deadlineDateAlertMessage')
        updateCharsLeft('deadlineName', 'deadlineNameLength', 30)
        $('#' + modalButton.getAttribute("id")).prop('hidden', false);
    })
}


/**
 * Customises the event modal attributes with depending on what event it should display and whether it's being
 * used for adding or editing a event. Also greys out the required dates to ensure only the dates in the
 * project date range are selectable.
 */
function eventModalSetup() {
    const eventModal = document.getElementById('eventModal')
    eventModal.addEventListener('show.bs.modal', function (event) {
        // Button that triggered the modal
        const button = event.relatedTarget

        // Extract info from data-bs-* attributes
        const events = JSON.parse(button.getAttribute('data-bs-event'))

        const type = button.getAttribute('data-bs-type')
        // Update the modal's content.
        const modalTitle = eventModal.querySelector('.modal-title')
        const modalBodyInputs = eventModal.querySelectorAll('.modal-body input')
        const modalButton = eventModal.querySelector('.modal-footer button')
        const modalForm = eventModal.querySelector('form')

        if (type === 'add') {
            modalTitle.innerText = 'Add Event'
            modalButton.innerHTML = 'Add Event'
            modalForm.action = 'add-event'
        } else {
            modalTitle.innerText = 'Edit Event'
            modalButton.innerHTML = 'Save Event'
            modalForm.action = `edit-event/${events.id}`
            modalForm.setAttribute('event', events)
        }

        modalForm.setAttribute('object', events);

        modalBodyInputs[0].value = events.eventName
        const startDate = moment(`${events.startDateDetail}`, 'DD/MMM/yyyy h:mm a').toDate();
        eventStartDatePicker.dates.setValue(tempusDominus.DateTime.convert(startDate));
        const endDate = moment(`${events.endDateDetail}`, 'DD/MMM/yyyy h:mm a').toDate();
        eventEndDatePicker.dates.setValue(tempusDominus.DateTime.convert(endDate));
        // Set min and max dates based on project dates
        eventStartDatePicker.updateOptions({restrictions: {minDate: new Date(projectStartDate), maxDate: new Date(projectEndDate)}});
        eventEndDatePicker.updateOptions({restrictions: {minDate: new Date(projectStartDate), maxDate: new Date(projectEndDate)}});



        // Initial run of validation functions in case initial values are invalid
        validateModalDateTimeRange('eventStartDate', 'eventEndDate', 'eventModalButton', 'eventDateTimeAlertBanner', 'eventDateTimeAlertMessage')
        updateCharsLeft('eventName', 'eventNameLength', 30)
        $('#' + modalButton.getAttribute("id")).prop('hidden', false);
    })

    eventModal.addEventListener('hidden.bs.modal', function (ignoredEvent) {
        $("#eventAlertMessage").val(null);
        $("#eventAlertBanner").setAttribute("hidden", "true")
        $("#eventDateTimeAlertMessage").val(null);
        $("#eventDateTimeAlertBanner").setAttribute("hidden", "true")
    });
}