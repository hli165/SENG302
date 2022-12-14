/**
 * Id of the non members group.
 * @type {number}
 */
const NON_MEMBER_GROUP_ID = 1

/**
 * Add an 'active' class to the selected group, highlight current selected group for better user experience
 */
function groupButtonSetup() {
    $('.group-bar button').on('click', function () {
        $("div.group-bar button").removeClass('active');
        $(this).addClass("active");
    })
}

/**
 * Reloads the selected group and its table.
 * @param groupId group to fetch information of
 */
function updateTable(groupId) {
    const url = "groups/local?";
    $('#tableRefreshContainer').load(url, "groupId=" + groupId)
}

/**
 * Redirect user to the group settings page.
 * @param groupId group to fetch information of.
 */
function redirectToSettingsPage(groupId) {
    const url = 'groupSettings?groupId=' + groupId
    document.location.href = url
}

/**
 * Returns the id of the currently selected group.
 * @returns {*} id of the currently selected group
 */
function getCurrentGroupId() {
    return $('#table_refresh').attr('data-groupid')
}

/**
 * Customises the group modal attributes with whether it's being
 * used for adding or editing a group.
 */
function groupModalSetup() {
    const groupModal = document.getElementById('groupModal')
    groupModal.addEventListener('show.bs.modal', function (event) {
        // Button that triggered the modal
        const button = event.relatedTarget

        // Extract info from data-bs-* attributes
        const id = button.getAttribute('data-bs-id')
        const shortName = button.getAttribute('data-bs-shortname')
        const longName = button.getAttribute('data-bs-longname')

        const type = button.getAttribute('data-bs-type')
        // Update the modal's content.
        const modalTitle = groupModal.querySelector('.modal-title')
        const modalBodyInputs = groupModal.querySelectorAll('.modal-body input')
        const modalButton = groupModal.querySelector('.modal-footer button')
        const modalForm = groupModal.querySelector('form')

        if (type === 'add') {
            modalTitle.innerText = 'Add Group'
            modalButton.innerHTML = 'Add Group'
            modalForm.action = 'add-group'
        } else {
            modalTitle.innerText = 'Edit Group'
            modalButton.innerHTML = 'Save Group'
            modalForm.action = `edit-group/${id}`
        }

        const modalAlerts = groupModal.querySelectorAll('.modal-body div.alert')
        modalAlerts.forEach(element => element.hidden = true)

        modalBodyInputs[0].value = shortName
        modalBodyInputs[1].value = longName

        updateCharsLeft('shortGroupNameInput', 'shortGroupNameLength', 10)
        updateCharsLeft('longGroupNameInput', 'longGroupNameLength', 30)
    })
 }

/**
 * Checks that the group modal inputs have text entered in them, then submits the group adding/editing form and adds
 * the new group to the page or updates the edited group if the action was successful, otherwise updates the modal
 * with error messages.
 * @returns {Promise<void>} null
 */
async function validateGroup() {
    if (validateModalName('shortGroupNameInput', 'groupShortNameAlertBanner', 'groupShortNameAlertMessage') &&
        validateModalName('longGroupNameInput', 'groupLongNameAlertBanner', 'groupLongNameAlertMessage')
    ) {
        document.getElementById('groupForm').onsubmit = () => { return false };
        removeInvalidCharacters('shortGroupNameInput')
        removeInvalidCharacters('longGroupNameInput')

        if (document.getElementById('groupForm').action.includes('add')) {
            addGroup()
        } else {
            editGroup()
        }

        document.getElementById('groupForm').onsubmit = () => {validateGroup(); return false}
    }
}

/**
 * Submits the group adding form and adds the new group to the page if the action was successful, otherwise updates
 * the modal with error messages.
 */
function addGroup() {
    const data = {
        shortName: document.getElementById('shortGroupNameInput').value,
        longName: document.getElementById('longGroupNameInput').value
    }
    $.post(document.getElementById('groupForm').action + "?" + new URLSearchParams(data)).done((result) => {
        $('#groupModal').modal('toggle')
        document.getElementById("groupList").innerHTML += result
        groupButtonSetup() // Allow group cards to be highlighted when selected
    }).fail(replaceGroupModalBody)
}

/**
 * Submits the group editing form and updates the edited group if the action was successful, otherwise updates the
 * modal with error messages.
 */
function editGroup() {
    const data = {
        shortName: document.getElementById('shortGroupNameInput').value,
        longName: document.getElementById('longGroupNameInput').value
    }
    const action = document.getElementById('groupForm').action
    $.post(action + "?" + new URLSearchParams(data)).done((result) => {
        sendNameReload(selectedArtefactName, selectedArtefactId, userId, userFirstName, userLastName, username, "Group")
        $('#groupModal').modal('toggle')
        const id = action.substring(action.lastIndexOf('/') + 1)
        $(`#groupCard${id}`).replaceWith(result)
        groupButtonSetup() // Allow group cards to be highlighted when selected
        $(`#groupCard${id} button`).click() // Select edited group
    }).fail(replaceGroupModalBody)
}

/**
 * Replaces the body of the group modal with the given modalBody response from the backend.
 * @param modalBodyResponse response with new modalBody to display (groupModalBody fragment)
 */
function replaceGroupModalBody(modalBodyResponse) {
    $("#groupModalBody").replaceWith(modalBodyResponse.responseText)
    updateCharsLeft('shortGroupNameInput', 'shortGroupNameLength', 10)
    updateCharsLeft('longGroupNameInput', 'longGroupNameLength', 30)
}

/**
 * Sets up the group delete modal so that the modal button triggers a method for deleting the group.
 */
function groupDeleteModalSetup() {
    const deleteModal = document.getElementById('deleteModal')
    deleteModal.addEventListener('show.bs.modal', function (event) {
        // Button that triggered the modal
        const button = event.relatedTarget

        // Extract info from data-bs-* attributes
        const id = button.getAttribute('data-bs-id')
        const type = button.getAttribute('data-bs-type')

        const modalButton = deleteModal.querySelector('.modal-footer button')
        const modalLink = deleteModal.querySelector('.modal-footer a')

        modalLink.removeAttribute('href')
        modalButton.onclick = () => {deleteGroupModalButtonFunction(type, id)}
    })
}

/**
 * Runs when the group delete modal button is pressed. Calls the endpoint for deleting the group. Disables the delete
 * button after being clicked. Removes the selected group upon success, and re-enables the delete button upon failure.
 * @param type the string 'group'
 * @param id id of the group
 */
function deleteGroupModalButtonFunction(type, id) {
    const buttonFunction = document.getElementById('deleteModalButton').onclick;
    document.getElementById('deleteModalButton').onclick = () => {return false}
    $.ajax({
        url: `delete-${type}/${id}`,
        type: 'DELETE',
        success: function() {
            $('#deleteModal').modal('toggle')
            $(`#groupCard${id}`).remove()
            $(`#groupCard1 button`).click() // Select members without a group
        },
        error: function() {
            document.getElementById('deleteModalButton').onclick = buttonFunction;
        }
    })
}

/**
 * Copies the users currently selected in the table to the group currently selected in the group dropdown.
 * After this action is completed the relevant groups and tables displayed to the user are updated.
 * A toast indicating success or failure is also displayed.
 */
function copyUsers() {
    const originGroupId = getCurrentGroupId()
    const groupName = $( "#groupDropdownList option:selected" ).text()
    const data = {
        groupId: document.getElementById('groupDropdownList').value,
        userIds: userTable.rows('.selected').data().toArray().map(row => row.DT_RowId),
        currentGroupId: originGroupId
    }
    $.post('copy-users' + "?" + new URLSearchParams(data)).done((result) => {
        if (data.groupId === '1') {
            $(`#groupList`).replaceWith(result)
            highlightCurrentGroup()
            updateTable(originGroupId)
        } else {
            $(`#groupCard${data.groupId}`).replaceWith(result)
            if (originGroupId === '1') {
                updateMembersWithoutAGroupCard()
                updateTable(originGroupId)
            }
        }
        changeMembersReload(originGroupId, data.groupId);
        groupButtonSetup() // Allow group cards to be highlighted when selected
        showAlertToast("Group " + groupName + " Updated")
    }).fail(() => {
        showAlertErrorToast("Group " + groupName + " failed to be updated")
    })
}

/**
 * Highlights the currently selected group. This is needed as sometimes the selected group is not highlighted when the
 * table is reloaded.
 */
function highlightCurrentGroup() {
    const groupId = getCurrentGroupId()
    document.getElementById("groupCard" + groupId).firstElementChild.classList.add("active")
}

/**
 * Requests an up-to-date version of the members without a group card and replaces the current card with it.
 */
function updateMembersWithoutAGroupCard() {
    $.get('members-without-a-group').done((result) => {
        $(`#groupCard1`).replaceWith(result)
        groupButtonSetup() // Allow group cards to be highlighted when selected
        highlightCurrentGroup()
    })
}

/**
 * Sends a websocket message to reload users pages when members are removed from groups.
 */
function reloadRemovedUsers() {
    const currentGroupId = parseInt($("#table_refresh").attr("data-groupid"), 10);
    changeMembersReload(currentGroupId, NON_MEMBER_GROUP_ID);
}

/**
 * Requests an up-to-date version of the group card list and updates the current list with it.
 * Also updates the members table if the current group selected was changed.
 */
function updateGroupList(groupId, action) {
    $.get('group-list').done((result) => {
        $(`#groupList`).replaceWith(result)
        groupButtonSetup() // Allow group cards to be highlighted when selected
    })
    if (groupId === parseInt($("#table_refresh").attr("data-groupid"), 10)) {
        const table = $("#table").DataTable();
        const url = "groups/local?";
        const pageNumber = table.page.info().page;
        sessionStorage.setItem("selected-members-page", pageNumber)
        let chosenGroupId = groupId;
        if (action === "delete") {
            chosenGroupId = 1;
            $('#tableRefreshContainer').load(url, "groupId=" + chosenGroupId)
        } else {
            $('#tableRefreshContainer').load(url, "groupId=" + chosenGroupId, () => {
                let newPageNumber = sessionStorage.getItem("selected-members-page");
                const table = $("#table").DataTable();
                // Makes sure the table is on the same page as before the reload
                if (action === "change-users-receive") {
                    table.page(parseInt(newPageNumber, 10)).draw(false);
                }
                if (action === "change-users-send") {
                    let maxPages = table.page.info().pages;
                    if (newPageNumber >= maxPages) {
                        newPageNumber = newPageNumber - 1;
                    }
                    if (maxPages !== 0 && newPageNumber > 0) {
                        table.page(parseInt(newPageNumber, 10)).draw(false);
                    }
                }
            })
        }

    }
}

/**
 * Sets up the modal for removing a user from a group.
 */
function removeUserModalSetup() {
    const removeUserModal = document.getElementById('removeUserModal')
    removeUserModal.addEventListener('show.bs.modal', function () {
        const modalButton = removeUserModal.querySelector('.modal-footer button')
        const modalLink = removeUserModal.querySelector('.modal-footer a')

        modalLink.removeAttribute('href')
        modalButton.onclick = () => {removeUserModalButtonFunction()}
    })
}

/**
 * Runs when the remove user modal button is pressed. Calls the endpoint for removing the user. Disables the remove
 * button after being clicked. Removes the selected user upon success, and re-enables the remove button upon failure.
 */
function removeUserModalButtonFunction() {
    const buttonFunction = document.getElementById('removeUserModalButton').onclick
    document.getElementById('removeUserModalButton').onclick = () => {return false}

    const selected = userTable.rows('.selected').data().toArray().map(row => row.DT_RowId)
    const groupId = getCurrentGroupId()
    const data = {groupId: groupId, userIds: selected}
    const groupName = document.getElementById('shortGroupNameInput').innerText
    $.post('remove-users' + "?" + new URLSearchParams(data)).done((result) => {
            if (data.groupId === '1') {
                $(`#groupList`).replaceWith(result)
                updateTable(groupId)
            } else {
                $(`#groupCard${data.groupId}`).replaceWith(result)
                if (groupId === '1') {
                    updateMembersWithoutAGroupCard()
                    updateTable(groupId)
                }
            }
            updateTable(groupId)
            updateMembersWithoutAGroupCard()
            $('#removeUserModal').modal('toggle')
            showAlertToast("Group " + groupName + " Updated")
            reloadRemovedUsers()
        }).fail(() => {
        document.getElementById('removeUserModalButton').onclick = buttonFunction;
        showAlertErrorToast("Group " + groupName + " failed to be updated")
    })
}