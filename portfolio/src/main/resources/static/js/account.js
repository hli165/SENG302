
function settingsMenuToggle() {
    var settingmenu = document.querySelector(".setting-menu");
    settingmenu.classList.toggle('setting-menu-height');
    settingmenu.addEventListener('click',function() {event.stopPropagation()})
}

/**
 * Sets up the delete modal to display the correct text and method/action to run for deleting a profile image.
 */
function deleteModalSetup() {
    const deleteModal = document.getElementById('deleteModal')
    deleteModal.addEventListener('show.bs.modal', function (event) {
        // Button that triggered the modal
        const button = event.relatedTarget;

        // Update the modal's content.
        const modalTitle = deleteModal.querySelector('.modal-title');
        const modalBodyLabel = deleteModal.querySelector('.modal-body label');
        const modalButton = deleteModal.querySelector('.modal-footer button');
        const modalLink = deleteModal.querySelector('.modal-footer a');
        const modalType = button.getAttribute('data-bs-type');
        const name = button.getAttribute('data-bs-name');


        if (modalType === 'photo') {
            modalTitle.innerText = `Delete Profile Photo`;
            modalBodyLabel.textContent = `Are you sure you want to delete your profile image?`;
            modalButton.innerHTML = `Delete`;
            modalLink.href = `deleteAccountPhoto`;

            $("#deleteModalButton").on('click', function (ignore) {
                let form = document.getElementById("baseForm");
                form.setAttribute("action", `@{deleteAccountPhoto}`);
                form.setAttribute("method", `get`)
                form.submit();
            });

        } else if (modalType === 'evidence') {
            const id = button.getAttribute('data-bs-id')
            modalTitle.innerText = `Delete Evidence`;
            modalBodyLabel.textContent = `Are you sure you want to delete the evidence piece '${name}'?`;
            modalButton.innerHTML = `Delete`;

            modalLink.removeAttribute('href')
            modalButton.onclick = () => {deleteEvidenceModalListener(id)}
        }
    })
}


function deleteEvidenceModalListener(id) {
    document.getElementById('deleteModalButton').onclick = () => {return false}
    $.ajax({
        url: `delete-evidence/${id}`,
        type: 'POST',
        success: function() {
            $('#deleteModal').modal('toggle')
            showAlertToast("Evidence deleted successfully!");
            sendDeleteEvidenceNotification();
            setTimeout(function() {
                $("#evidenceList").load(url +" #evidenceList>*","", function (data) {

                    if (data.includes("<title>Account Page</title>") && viewableUser !== "") {
                        redirectToEvidence(viewableUser);
                    }
                });
            }, 10);
        },
        error: function(error) {
            $('#deleteModal').modal('toggle');
            showAlertErrorToast("Something went wrong went deleting evidence!");
        }
    })
}