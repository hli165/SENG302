/**
 * Toggles the icon style being used for the arrow.
 *
 * @param {string} arrowId  The id of the arrow to toggle.
 */
function changeIcon(arrowId) {
    let icon = $(arrowId);
    icon.toggleClass("bi bi-caret-down-fill bi bi-caret-left-fill")
    let iconHtml = document.getElementById(arrowId.getAttribute('id'))
    if (iconHtml.getAttribute("class").includes("left")) {
        sessionStorage.setItem(icon.attr('id'), "false");
    } else {
        sessionStorage.setItem(icon.attr('id'), "true");
    }

}

/**
 * Toggles the collapse of the element.
 * @param collapseButtonId  The id of the button that toggles the collapse.
 * @param arrowId  The id of the arrow to toggle.
 * @param collapseId Id of the element to collapse, to add the animation event listeners to flip the arrows.
 */
function setToggle(collapseButtonId, arrowId, collapseId) {
    let collapse = document.getElementById(collapseId);
    collapse.addEventListener("show.bs.collapse", function (event) {
        let svgArray = Array.from(event.target.parentElement.getElementsByTagName("svg"));
        let arrow = svgArray.find(svg => svg.id.includes("Arrow"));
        let iconHtml = document.getElementById(arrow.getAttribute('id'));
        iconHtml.getElementsByTagName("path").item(0).setAttribute('d', "M7.247 11.14 2.451 5.658C1.885 5.013 2.345 4 3.204 4h9.592a1 1 0 0 1 .753 1.659l-4.796 5.48a1 1 0 0 1-1.506 0z");
    });
    collapse.addEventListener("hide.bs.collapse", function (event) {
        let svgArray = Array.from(event.target.parentElement.getElementsByTagName("svg"));
        let arrow = svgArray.find(svg => svg.id.includes("Arrow"));
        let iconHtml = document.getElementById(arrow.getAttribute('id'));
        iconHtml.getElementsByTagName("path").item(0).setAttribute('d', "m3.86 8.753 5.482 4.796c.646.566 1.658.106 1.658-.753V3.204a1 1 0 0 0-1.659-.753l-5.48 4.796a1 1 0 0 0 0 1.506z");
    });
    let shouldToggle = sessionStorage.getItem(arrowId);
    if (shouldToggle === "true") {
        document.getElementById(collapseButtonId).click();
    }
}

