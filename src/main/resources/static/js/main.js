const THUMBNAIL_SIZE = 400;

const gallery = document.getElementById('gallery');
const fullsizeImage = document.getElementById('fullsize-image');
const fullsizeContainer = document.getElementById('fullsize-container');
const fullsizePrev = document.getElementById('fullsize-prev');
const fullsizeNext = document.getElementById('fullsize-next');

let currentPage = 0;
let totalPages = 0;

let currentlyActiveFullscreenImageId = null;

let orderBy = 'date';
let orderAsc = false;
let orderByIncludeVideos = false;

// Fetch the total number of pages
axios.get('/media/page/count/' + orderByIncludeVideos)
    .then(response => {
        totalPages = response.data.total;
        console.log(`Total pages: ${totalPages}`);
        loadNextPage();
    });

// Create an Intersection Observer to load the next page when the user scrolls to the bottom
const pageObserver = new IntersectionObserver(entries => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            pageObserver.unobserve(entry.target);
            loadNextPage();
        }
    });
});

// Create another Intersection Observer to load and unload images/videos as they scroll into and out of view
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
    rootMargin: '200px'  // Load/unload images/videos when they're within 200px of the visible area
});

function loadNextPage() {
    if (currentPage < totalPages) {
        axios.get(`/media/page/${currentPage}/${orderBy}/${orderAsc}/${orderByIncludeVideos}`)
            .then(response => {
                let addedItems = [];
                response.data.ids.forEach(id => {
                    const placeholder = document.createElement('div');
                    placeholder.className = 'gallery-item-container';
                    placeholder.dataset.id = id;
                    gallery.appendChild(placeholder);
                    imageObserver.observe(placeholder);
                    addedItems.push(placeholder);
                    placeholder.onclick = () => {
                        showFullSizeImage(placeholder);
                    };
                });
                currentPage++;

                // Observe the last image placeholder to trigger the pageObserver
                // get the 10th item from the end of the array, or if there are less than 10 items, get the first one
                const lastPlaceholder = addedItems[addedItems.length - 10] || addedItems[0];
                if (lastPlaceholder) {
                    pageObserver.observe(lastPlaceholder);
                }
            });
    }
}

function loadImage(placeholder) {
    if (placeholder.querySelector('img')) {
        return;
    }

    const id = placeholder.dataset.id;
    const img = document.createElement('img');
    img.src = `/media/get/${id}/thumb/${THUMBNAIL_SIZE}`;
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
    currentPage = 0;
    totalPages = 0;
    while (gallery.firstChild) {
        gallery.removeChild(gallery.firstChild);
    }
    axios.get('/media/page/count/' + orderByIncludeVideos)
        .then(response => {
            totalPages = response.data.total;
            console.log(`Total pages: ${totalPages}`);
            loadNextPage();
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
            currentlyActiveFullscreenImageId = id;
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
            currentlyActiveFullscreenImageId = id;
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
}

function hideFullSizeImage() {
    fullsizeContainer.classList.remove('open');
    fullsizeImage.classList.remove('loaded');
    while (fullsizeImage.firstChild) {
        fullsizeImage.removeChild(fullsizeImage.firstChild);
    }
    currentlyActiveFullscreenImageId = null;
}

function getNextFullSizeImageId() {
    return document.querySelector(`.gallery-item-container[data-id="${currentlyActiveFullscreenImageId}"]`).nextSibling;
}

function nextFullSizeImage() {
    const nextImage = getNextFullSizeImageId();
    console.log(nextImage)
    if (nextImage) {
        nextImage.scrollIntoView();
        showFullSizeImage(nextImage);
    } else {
        console.log('loading next page to find next image')
        loadNextPage();
    }
}

function getPreviousFullSizeImageId() {
    return document.querySelector(`.gallery-item-container[data-id="${currentlyActiveFullscreenImageId}"]`).previousSibling;
}

function previousFullSizeImage() {
    const previousImage = getPreviousFullSizeImageId();
    console.log(previousImage)
    if (previousImage) {
        previousImage.scrollIntoView();
        showFullSizeImage(previousImage);
    }
}

fullsizeContainer.onclick = e => {
    if (e.target !== fullsizePrev && e.target !== fullsizeNext) {
        hideFullSizeImage();
    }
}
fullsizePrev.onclick = () => {
    previousFullSizeImage();
}
fullsizeNext.onclick = () => {
    nextFullSizeImage();
}

document.addEventListener('keydown', (event) => {
    if (event.key === "Escape") {
        hideFullSizeImage();
    } else if (event.key === "ArrowLeft") {
        previousFullSizeImage();
    } else if (event.key === "ArrowRight") {
        nextFullSizeImage();
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

function disableWebpage() {
    enableWebpage();
    const disableWebpage = document.createElement('div');
    disableWebpage.classList.add('disable-webpage');
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
    const myModal = new bootstrap.Modal(document.getElementById('settingsModal'), {});
    myModal.show();
    populateSettingsModalData();
}

function hideSettingsModal() {
    const myModal = bootstrap.Modal.getInstance(document.getElementById('settingsModal'));
    myModal.hide();
}

// {"settings":{"image_directories":["D:\\files\\media\\images\\screenshots\\nintendo_switch","D:\\files\\media\\images\\screenshots\\games"],"index_on_startup":false}}
function populateSettingsModalData() {
    axios.get('/settings/get')
        .then(response => {
            const settings = response.data.settings;
            console.log(settings);
            const imageDirectories = settings.image_directories;
            const disabledImageDirectories = settings.disabled_image_directories;
            const indexOnStartup = settings.index_on_startup;

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
