/**
 * Returns true if the key pressed is a valid key for dates, otherwise false.
 * Valid keys are numbers, letters, forward slashes, backspace, enter, and delete.
 * @param event key press event
 * @returns {boolean} true if the key pressed is a valid key for dates, otherwise false
 */
function checkDateKeys(event) {
    return '1234567890/'.includes(event.key) || [8, 13, 46].includes(event.keyCode) || 65 <= event.keyCode && event.keyCode <= 90;
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
        $('#sprintStart').datepicker('setDate', sprint.startDateString)
        $('#sprintEnd').datepicker('setDate', sprint.endDateString)
        modalBodyTextArea.value = sprint.description

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

        // Update the modal's content.
        const modalTitle = deleteModal.querySelector('.modal-title')
        const modalBodyLabel = deleteModal.querySelector('.modal-body label')
        const modalButton = deleteModal.querySelector('.modal-footer button')
        const modalLink = deleteModal.querySelector('.modal-footer a')

        modalTitle.innerText = `Delete ${type.charAt(0).toUpperCase() + type.slice(1)}`
        modalBodyLabel.textContent = `Are you sure you want to delete ${name}?`
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
        $('#projectStart').datepicker('setDate', project.startDateString)
        $('#projectEnd').datepicker('setDate', project.endDateString)
        modalBodyTextArea.value = project.description

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
        console.log(milestone)
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
            modalForm.setAttribute('data-milestone-id', sprint.id)
        }

        modalForm.setAttribute('object', milestone);
        modalBodyInput.value = milestone.milestoneName
        $('#milestoneDateInput').datepicker('setDate', milestone.milestoneDateString)

        // Initial run of validation functions in case initial values are invalid
        validateModalName('milestoneName', 'milestoneModalButton', 'milestoneAlertBanner', 'milestoneAlertMessage')
        validateModalDate('milestoneDate', 'milestoneModalButton', 'milestoneDateAlertBanner', 'milestoneDateAlertMessage')
        updateCharsLeft('milestoneName', 'milestoneNameLength', 50)
    })
}