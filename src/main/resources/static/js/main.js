const DEFAULT_THUMBNAIL_SIZE = 400;

const gallery = document.getElementById('gallery');
const fullsizeImage = document.getElementById('fullsize-image');
const fullsizeContainer = document.getElementById('fullsize-container');
const fullsizePrev = document.getElementById('fullsize-prev');
const fullsizeNext = document.getElementById('fullsize-next');
const jumpToTableBody = document.getElementById('jump-to-table-body');
const galleryNavigationHoverElement = document.getElementById('gallery-navigation');
const galleryNavigationTextDisplay = document.getElementById('gallery-navigation-popover');
const galleryNavigationTextDisplayDescription = document.getElementById('gallery-navigation-popover-content-description');
const galleryNavigationTextDisplayDate = document.getElementById('gallery-navigation-popover-content-date');
const galleryNavigationCurrentIndex = document.getElementById('gallery-navigation-current-index');

let totalPages = 0;
let pagesToBeLoaded = [];

let pagesSummaryEntries = [];
let currentlyActiveFullscreenImageId = null;
let isCurrentlyJumpingToPage = false;

let orderBy = 'date';
let orderAsc = false;
let orderByIncludeVideos = false;
let thumbnailSize = DEFAULT_THUMBNAIL_SIZE;

softReloadPage();

// Intersection Observer to load the next page when the user scrolls to the bottom
const pageObserver = new IntersectionObserver(entries => {
    entries.forEach(entry => {
        if (entry.isIntersecting && !isCurrentlyJumpingToPage) {
            pageObserver.unobserve(entry.target);
            loadPage(parseInt(entry.target.dataset.loadPageIndex));
        }
    });
});

// Intersection Observer to load and unload images/videos as they scroll into and out of view
const imageObserver = new IntersectionObserver(entries => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            loadImage(entry.target);
        } else {
            unloadImage(entry.target);
        }
    });
}, {
    root: null,
    rootMargin: '400px'  // load/unload images/videos when they're within a radius of the visible area
});

function loadPage(page, scrollIntoView = false) {
    if (page < 0) {
        return;
    }

    const isLargerThanTotalPages = page >= totalPages;
    const isNotToBeLoaded = !pagesToBeLoaded.includes(page);

    if (isLargerThanTotalPages || isNotToBeLoaded) {
        console.log(`Skipping loading page ${page}`)

        if (scrollIntoView) {
            scrollToPage(page);
        }
        return;
    }
    console.log(`Loading page ${page}...`)

    axios.get(`/media/page/${page}/${orderBy}/${orderAsc}/${orderByIncludeVideos}`)
        .then(response => {
            // find where to insert the new items by finding the last element with the page number 1 less than the current page
            let insertAfter = null;
            const galleryItemContainers = document.querySelectorAll('.gallery-item-container');
            const galleryItemContainersArray = Array.from(galleryItemContainers);

            let pageNumbers = galleryItemContainersArray.map(container => parseInt(container.dataset.page));
            let filteredPageNumbers = pageNumbers.filter(pageNumber => pageNumber < page);
            let maxPageLessThanCurrent = Math.max(...filteredPageNumbers);

            galleryItemContainersArray.forEach(container => {
                if (parseInt(container.dataset.page) === maxPageLessThanCurrent) {
                    insertAfter = container;
                }
            });

            const elementAtCenterOfScreen = getElementAtCenterOfScreen();
            const currentlyViewedPage = getPageOfElement(elementAtCenterOfScreen);
            const isCurrentlyViewedPageAfter = currentlyViewedPage > page;
            const scrollHeightOfElementAtCenterOfScreen = getDistanceFromTop(elementAtCenterOfScreen, gallery);


            insertAfter = insertAfter || galleryItemContainers[galleryItemContainers.length - 1] || null;

            let addedItems = [];
            response.data.ids.forEach(id => {
                const placeholder = document.createElement('div');
                placeholder.className = 'gallery-item-container';
                placeholder.dataset.id = id;
                placeholder.dataset.page = page;
                placeholder.style.height = `${Math.round(thumbnailSize / 1.7)}px`;
                imageObserver.observe(placeholder);
                addedItems.push(placeholder);
                placeholder.onclick = () => {
                    showFullSizeImage(placeholder);
                };

                // Insert the placeholder after the insertAfter item
                if (insertAfter === null) {
                    gallery.appendChild(placeholder);
                } else {
                    insertAfterElement(insertAfter, placeholder);
                }
                insertAfter = placeholder;
            });
            pagesToBeLoaded = pagesToBeLoaded.filter(p => p !== page);

            if (isCurrentlyViewedPageAfter && elementAtCenterOfScreen) {
                const scrollHeightOfElementAtCenterOfScreenAfterAddingElements = getDistanceFromTop(elementAtCenterOfScreen, gallery);
                const scrollDifference = scrollHeightOfElementAtCenterOfScreenAfterAddingElements - scrollHeightOfElementAtCenterOfScreen;
                window.scrollBy(0, scrollDifference);
                elementAtCenterOfScreen.scrollIntoView();
            }

            // Observe the last image placeholder to trigger the pageObserver
            // get the 10th item from the end of the array, or if there are less than 10 items, get the first one
            const lastPlaceholder = addedItems[addedItems.length - 10] || addedItems[0];
            if (lastPlaceholder) {
                lastPlaceholder.dataset.loadPageIndex = "" + (page + 1);
                pageObserver.observe(lastPlaceholder);
            }
            // then observe the first image placeholder to trigger the pageObserver
            const firstPlaceholder = addedItems[0];
            if (firstPlaceholder) {
                firstPlaceholder.dataset.loadPageIndex = "" + (page - 1);
                pageObserver.observe(firstPlaceholder);
            }

            if (scrollIntoView) {
                const scrollTo = findLastElement(addedItems);
                if (scrollTo) {
                    disableWebpage(3);
                    isCurrentlyJumpingToPage = true;
                    loadPage(page - 1, false);
                    loadImage(scrollTo);

                    let wasVisibleCount = 0;
                    let iterations = 0;
                    const inViewPadding = 100;
                    let interval = setInterval(() => {
                        scrollTo.scrollIntoView();
                        const rect = scrollTo.getBoundingClientRect();
                        const isInView = (
                            rect.top >= -inViewPadding &&
                            rect.left >= -inViewPadding &&
                            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) + inViewPadding &&
                            rect.right <= (window.innerWidth || document.documentElement.clientWidth) + inViewPadding
                        );
                        wasVisibleCount += isInView ? 1 : 0;
                        iterations++;
                        if (wasVisibleCount > 10 || iterations > 100) {
                            enableWebpage();
                            isCurrentlyJumpingToPage = false;
                            clearInterval(interval);
                        }
                    }, 100);
                }
            }
        });
}

function scrollToPage(page) {
    const element = findLastElement(Array.from(document.querySelectorAll('.gallery-item-container')).filter(container => container.dataset.page === "" + page));
    if (element) {
        element.scrollIntoView();
        return true;
    }
    return false;
}

function getPageOfElement(element) {
    if (element) {
        return parseInt(element.dataset.page);
    }
    return 0;
}

function getElementAtCenterOfScreen() {
    const galleryItemContainers = document.querySelectorAll('.gallery-item-container');
    //const containersWithImg = Array.from(galleryItemContainers).filter(container => container.querySelector('img'));
    const containersWithImg = filterVisibleElements(Array.from(galleryItemContainers));

    if (containersWithImg.length > 0) {
        const middleIndex = Math.floor(containersWithImg.length / 2);
        return containersWithImg[middleIndex];
    }
    return null;
}

function filterVisibleElements(elements) {
    return elements.filter(element => {
        const rect = element.getBoundingClientRect();
        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
            rect.right <= (window.innerWidth || document.documentElement.clientWidth)
        );
    });
}

function getDistanceFromTop(element, scrollParent) {
    if (element === null) {
        return 0;
    }
    return element.offsetTop - scrollParent.scrollTop;
}

function findFirstElement(elements) {
    return elements.reduce((first, current) => {
        // compareDocumentPosition returns 4 if first is following current
        return first.compareDocumentPosition(current) & Node.DOCUMENT_POSITION_FOLLOWING ? current : first;
    });
}

function findLastElement(elements) {
    return elements.reduce((last, current) => {
        // compareDocumentPosition returns 2 if first is preceding current
        return last.compareDocumentPosition(current) & Node.DOCUMENT_POSITION_PRECEDING ? current : last;
    });
}

function insertAfterElement(existingNode, newNode) {
    existingNode.parentNode.insertBefore(newNode, existingNode.nextSibling);
}

galleryNavigationHoverElement.addEventListener('mousemove', e => handleNavigationHover(e.clientX, e.clientY, false));
galleryNavigationHoverElement.addEventListener('touchmove', e => handleNavigationHover(e.touches[0].clientX, e.touches[0].clientY, false));
galleryNavigationHoverElement.addEventListener('click', e => handleNavigationHover(e.clientX, e.clientY, true));

function handleNavigationHover(x, y, clicked = false) {
    const screenHeight = window.innerHeight;
    const percentage = y / screenHeight;
    const pagesSummaryEntriesIndex = Math.floor(percentage * pagesSummaryEntries.length);
    const pagesSummaryEntry = pagesSummaryEntries[pagesSummaryEntriesIndex];

    galleryNavigationTextDisplay.style.top = `${Math.max(Math.min(y - 20, screenHeight - 66), 10)}px`;
    if (pagesSummaryEntry) {
        galleryNavigationTextDisplayDescription.innerText = pagesSummaryEntry.file;
        galleryNavigationTextDisplayDate.innerText = pagesSummaryEntry.date;

        if (clicked) {
            loadPage(pagesSummaryEntry.page, true);
        }
    }
}

let navigationScrollTimeout = null;

document.addEventListener('scroll', handleNavigationPageScroll);

function handleNavigationPageScroll(e) {
    navigationScrollTimeout && clearTimeout(navigationScrollTimeout);

    navigationScrollTimeout = setTimeout(() => {
        const elementAtCenterOfScreen = getElementAtCenterOfScreen();
        const currentlyViewedPage = getPageOfElement(elementAtCenterOfScreen);

        const percentage = currentlyViewedPage / totalPages;
        const screenHeight = window.innerHeight;
        const y = percentage * screenHeight;
        galleryNavigationCurrentIndex.style.top = `${y}px`;
    }, 400);
}

function loadImage(placeholder) {
    if (placeholder.querySelector('img')) {
        return;
    }

    const id = placeholder.dataset.id;
    const img = document.createElement('img');
    img.src = `/media/get/${id}/thumb/${thumbnailSize}`;
    img.onload = () => {
        placeholder.style.height = 'auto';
        img.onclick = () => {
            showFullSizeImage(placeholder);
        };

        axios.get(`/media/get/${id}/type`)
            .then(response => {
                if (response.data.type === 'vid') {
                    placeholder.classList.add('video');
                }
            });
    };
    img.onerror = (error) => {
        console.error(`Error loading image with ID ${id}:`, error);
        placeholder.removeChild(img);
    };
    placeholder.appendChild(img);
}

function unloadImage(placeholder) {
    const img = placeholder.querySelector('img');
    if (img) {
        placeholder.style.height = `${img.offsetHeight}px`;
        placeholder.removeChild(img);
    }
    while (placeholder.firstChild) {
        placeholder.removeChild(placeholder.firstChild);
    }
}

function softReloadPage() {
    pagesToBeLoaded = [];
    totalPages = 0;
    isCurrentlyJumpingToPage = false;
    while (gallery.firstChild) {
        pageObserver.unobserve(gallery.firstChild)
        imageObserver.unobserve(gallery.firstChild)
        gallery.removeChild(gallery.firstChild);
    }
    axios.get('/media/page/count/' + orderByIncludeVideos)
        .then(response => {
            totalPages = parseInt(response.data.total);
            pagesToBeLoaded = Array.from({length: totalPages}, (_, i) => i);
            console.log(`Total pages: ${totalPages}`);
            loadPage(0);
        });
    axios.get(`/media/summary/${orderBy}/${orderAsc}/${orderByIncludeVideos}`)
        .then(response => {
            jumpToTableBody.innerHTML = '';

            pagesSummaryEntries = response.data.media;

            response.data.media.forEach((item, index) => {
                const row = jumpToTableBody.insertRow();
                const dateCell = row.insertCell();
                const fileCell = row.insertCell();

                dateCell.innerText = item.date;
                fileCell.innerText = item.file;

                row.onclick = () => {
                    loadPage(item.page, true);
                };
                row.classList.add('clickable');
                fileCell.classList.add('code');
            });
        });
}

function showFullSizeImage(placeholder) {
    if (!fullsizeContainer.classList.contains('open')) {
        while (fullsizeImage.firstChild) {
            fullsizeImage.removeChild(fullsizeImage.firstChild);
        }
    }

    const id = placeholder.dataset.id;

    if (placeholder.classList.contains('video')) {
        const video = document.createElement('video');
        video.src = `/media/get/${id}/full`;
        video.controls = true;
        video.autoplay = true;
        video.onloadedmetadata = () => {
            fullsizeImage.appendChild(video);
            while (fullsizeImage.firstChild !== video) {
                fullsizeImage.removeChild(fullsizeImage.firstChild);
            }
            fullsizeImage.classList.add('loaded');
        };
        video.onerror = (error) => {
            console.error(`Error loading video with ID ${id}:`, error);
            fullsizeImage.removeChild(video);
            fullsizeContainer.classList.remove('open');
            fullsizeImage.classList.remove('loaded');
            currentlyActiveFullscreenImageId = null;
        };
        fullsizeContainer.classList.add('open');

    } else {
        const img = document.createElement('img');

        img.src = `/media/get/${id}/full`;
        img.onload = () => {
            fullsizeImage.appendChild(img);
            while (fullsizeImage.firstChild !== img) {
                fullsizeImage.removeChild(fullsizeImage.firstChild);
            }
            fullsizeImage.classList.add('loaded');
        };
        img.onerror = (error) => {
            console.error(`Error loading image with ID ${id}:`, error);
            fullsizeImage.removeChild(img);
            fullsizeContainer.classList.remove('open');
            fullsizeImage.classList.remove('loaded');
            currentlyActiveFullscreenImageId = null;
        };
        fullsizeContainer.classList.add('open');
    }

    currentlyActiveFullscreenImageId = id;
}

function hideFullSizeImage() {
    fullsizeContainer.classList.remove('open');
    fullsizeImage.classList.remove('loaded');
    while (fullsizeImage.firstChild) {
        fullsizeImage.removeChild(fullsizeImage.firstChild);
    }
    currentlyActiveFullscreenImageId = null;
}

function openInEnclosingFolder(id) {
    axios.get(`/system/show-in-folder/${id}`);
}

function getNextFullSizeImageId(id = currentlyActiveFullscreenImageId) {
    return document.querySelector(`.gallery-item-container[data-id="${id}"]`).nextSibling;
}

function getPreviousFullSizeImageId(id = currentlyActiveFullscreenImageId) {
    return document.querySelector(`.gallery-item-container[data-id="${id}"]`).previousSibling;
}

function nextFullSizeImage() {
    const nextImage = getNextFullSizeImageId();
    if (nextImage) {
        nextImage.scrollIntoView();
        try {
            tryToPreloadImage(`/media/get/${nextImage.nextSibling.dataset.id}/full`);
            const nextNextImage = nextImage.nextSibling;
            if (nextNextImage) {
                tryToPreloadImage(`/media/get/${nextNextImage.nextSibling.dataset.id}/full`);
            }
        } catch (e) {
            console.log(e);
        }
        showFullSizeImage(nextImage);
    }
}

function previousFullSizeImage() {
    const previousImage = getPreviousFullSizeImageId();
    if (previousImage) {
        previousImage.scrollIntoView();
        try {
            tryToPreloadImage(`/media/get/${previousImage.previousSibling.dataset.id}/full`);
            const previousPreviousImage = previousImage.previousSibling;
            if (previousPreviousImage) {
                tryToPreloadImage(`/media/get/${previousPreviousImage.previousSibling.dataset.id}/full`);
            }
        } catch (e) {
            console.log(e);
        }
        showFullSizeImage(previousImage);
    }
}

function isFullscreenActive() {
    return fullsizeContainer.classList.contains('open');
}

function isSettingsModalActive() {
    return document.querySelector('#settingsModal.show') !== null;
}

function isNavigationModalActive() {
    return document.querySelector('#jumpToPageModal.show') !== null;
}

fullsizeContainer.onclick = e => {
    if (e.target !== fullsizePrev && e.target !== fullsizeNext) {
        if (e.detail === 2) { // check for double click
            openInEnclosingFolder(currentlyActiveFullscreenImageId);
        } else if (e.button === 0) { // left click
            hideFullSizeImage();
        }
    }
}
fullsizePrev.onclick = () => {
    previousFullSizeImage();
}
fullsizeNext.onclick = () => {
    nextFullSizeImage();
}

document.addEventListener('keydown', (event) => {
    const settingsModalActive = isSettingsModalActive();
    const navigationModalActive = isNavigationModalActive();
    const fullscreenActive = isFullscreenActive();
    const userIsInteractingWithInput = document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA';
    const userIsInteractingWithCheckbox = document.activeElement.tagName === 'INPUT' && document.activeElement.type === 'checkbox';

    if (fullscreenActive) {
        if (event.key === "Escape") {
            hideFullSizeImage();
        } else if (event.key === "ArrowLeft") {
            previousFullSizeImage();
        } else if (event.key === "ArrowRight") {
            nextFullSizeImage();
        }
    }
    if (settingsModalActive) {
        if (event.key === "Escape" || ((!userIsInteractingWithInput || userIsInteractingWithCheckbox) && event.key === "s")) {
            hideSettingsModal();
        }
    } else if (navigationModalActive) {
        if (event.key === "Escape" || ((!userIsInteractingWithInput || userIsInteractingWithCheckbox) && event.key === "n")) {
            hideJumpToModal();
        }
    } else {
        if (event.key === "s") {
            openSettingsModal();
        } else if (event.key === "n") {
            openJumpToModal();
        }
    }
});

document.addEventListener('touchstart', handleTouchStart, false);
document.addEventListener('touchmove', handleTouchMove, false);

let xDown = null;
let yDown = null;

function getTouches(evt) {
    return evt.touches || evt.originalEvent.touches;
}

function handleTouchStart(evt) {
    const firstTouch = getTouches(evt)[0];
    xDown = firstTouch.clientX;
    yDown = firstTouch.clientY;
}

function handleTouchMove(evt) {
    if (!xDown || !yDown) {
        return;
    }

    const xUp = evt.touches[0].clientX;
    const yUp = evt.touches[0].clientY;

    const xDiff = xDown - xUp;
    const yDiff = yDown - yUp;

    if (Math.abs(xDiff) > Math.abs(yDiff)) {/*most significant*/
        if (xDiff > 0) {
            /* right swipe */
            nextFullSizeImage();
        } else {
            /* left swipe */
            previousFullSizeImage();
        }
    } else {
        if (yDiff > 0) {
            /* down swipe */
        } else {
            /* up swipe */
        }
    }
    /* reset values */
    xDown = null;
    yDown = null;
}

function disableWebpage(level = 1) {
    enableWebpage();
    const disableWebpage = document.createElement('div');
    disableWebpage.classList.add('disable-webpage');
    if (level === 1) {
        disableWebpage.classList.add('l1');
    } else if (level === 2) {
        disableWebpage.classList.add('l2');
    } else if (level === 3) {
        disableWebpage.classList.add('l3');
    }
    disableWebpage.onclick = () => false;
    document.body.appendChild(disableWebpage);
}

function enableWebpage() {
    const disableWebpage = document.querySelector('.disable-webpage');
    if (disableWebpage) {
        document.body.removeChild(disableWebpage);
    }
}

function openSettingsModal() {
    hideJumpToModal();
    const myModal = new bootstrap.Modal(document.getElementById('settingsModal'), {});
    myModal.show();
    populateSettingsModalData();
}

function hideSettingsModal() {
    const myModal = bootstrap.Modal.getInstance(document.getElementById('settingsModal'));
    if (myModal) {
        myModal.hide();
    }
}

function openJumpToModal() {
    hideSettingsModal();
    const myModal = new bootstrap.Modal(document.getElementById('jumpToPageModal'), {});
    myModal.show();
}

function hideJumpToModal() {
    const myModal = bootstrap.Modal.getInstance(document.getElementById('jumpToPageModal'));
    if (myModal) {
        myModal.hide();
    }
}

function populateSettingsModalData() {
    axios.get('/settings/get')
        .then(response => {
            const settings = response.data.settings || {};
            const imageDirectories = settings.image_directories || [];
            const disabledImageDirectories = settings.disabled_image_directories || [];
            const indexOnStartup = settings.index_on_startup || false;

            const table = document.getElementById('settings-table');
            const tbody = table.querySelector('tbody');
            tbody.innerHTML = '';

            imageDirectories.forEach((directory, index) => {
                const row = tbody.insertRow();
                const pathCell = row.insertCell();
                const optionsCell = row.insertCell();

                const isDisabled = disabledImageDirectories.includes(directory);

                pathCell.classList.add('code');

                pathCell.innerText = directory;
                optionsCell.innerHTML = `
                    <button type="button" class="btn btn-danger" title="Delete" onclick="removeImageDirectory('${directory.replaceAll('\\', '\\\\')}')">🗑️</button>
                    <button type="button" class="btn btn-primary" title="Rescan" onclick="rescanImageDirectory('${directory.replaceAll('\\', '\\\\')}')">🔄</button>
                    <button type="button" class="btn btn-primary" title="${isDisabled ? 'Enable' : 'Disable'}" onclick="setImageDirectoryActive('${directory.replaceAll('\\', '\\\\')}', ${isDisabled})">${isDisabled ? '❌' : '✔️'}</button>
                `;
            });

            const reindexOnStartup = document.getElementById('reindex-on-startup');
            reindexOnStartup.checked = indexOnStartup;
        });

    document.getElementById('order-by-direction').checked = orderAsc;
    document.getElementById('order-by-videos').checked = orderByIncludeVideos;

    document.getElementById('thumbnail-size').value = thumbnailSize;
}

function rescanImageDirectory(path) {
    disableWebpage();
    axios.post('/settings/path/rescan', {path})
        .then(response => {
            console.log(response);
            populateSettingsModalData();
            softReloadPage();
        })
        .finally(() => {
            enableWebpage();
        });
}

function removeImageDirectory(path) {
    disableWebpage();
    axios.post('/settings/path/remove', {path})
        .then(response => {
            console.log(response);
            populateSettingsModalData();
            softReloadPage();
        })
        .finally(() => {
            enableWebpage();
        });
}

function setImageDirectoryActive(path, active) {
    disableWebpage();
    let enabled = active ? 'enable' : 'disable';
    axios.post('/settings/path/' + enabled, {path})
        .then(response => {
            console.log(response);
            populateSettingsModalData();
            softReloadPage();
        })
        .finally(() => {
            enableWebpage();
        });
}

function addImagePath() {
    const path = prompt('Enter the path to the image directory');
    if (path) {
        disableWebpage();
        axios.post('/settings/path/add', {path})
            .then(response => {
                console.log(response);
                populateSettingsModalData();
                softReloadPage();
            })
            .finally(() => {
                enableWebpage();
            });
    }
}

function setReindexOnStartup(checked) {
    disableWebpage();
    axios.post('/settings/reindex-on-startup', {checked})
        .then(response => {
            console.log(response);
            populateSettingsModalData();
        })
        .finally(() => {
            enableWebpage();
        });
}

function sendApplicationShutdown(type) {
    const shutdownApplicationButton1 = document.getElementById('shutdown-application-cleanup-1');
    const shutdownApplicationButton2 = document.getElementById('shutdown-application-cleanup-2');
    shutdownApplicationButton1.disabled = true;
    shutdownApplicationButton2.disabled = true;

    axios.get('/system/shutdown/' + type)
        .then(response => {
            shutdownApplicationButton1.disabled = false;
            shutdownApplicationButton1.innerText = 'Failed to shutdown application';
            shutdownApplicationButton2.disabled = false;
            shutdownApplicationButton2.innerText = 'Failed to shutdown application';
        })
        .catch(error => {
            shutdownApplicationButton1.disabled = false;
            shutdownApplicationButton1.innerText = 'Application terminated successfully';
            shutdownApplicationButton2.disabled = false;
            shutdownApplicationButton2.innerText = 'Application terminated successfully';
        });
}

function setOrderBy(selection) {
    orderBy = selection;
    softReloadPage();
}

function setOrderByDirection(checked) {
    orderAsc = checked;
    softReloadPage();
}

function setOrderByIncludeVideos(checked) {
    orderByIncludeVideos = checked;
    softReloadPage();
}

function setThumbnailSize(size) {
    if (size > 100 && size < 1000) {
        thumbnailSize = size;
        document.getElementById('thumbnail-size').classList.remove('is-invalid');
        softReloadPage();
    } else {
        document.getElementById('thumbnail-size').classList.add('is-invalid');
    }
}

function tryToPreloadImage(src) {
    try {
        const image = new Image();
        image.src = src;
        image.style = "position: fixed; top: -600px; left: 0; width: 500px; height: 500px;"
        document.body.appendChild(image);
        setTimeout(() => {
            document.body.removeChild(image);
        }, 2000);
    } catch (e) {
        console.error(e);
    }
}
