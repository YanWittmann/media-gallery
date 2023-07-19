const THUMBNAIL_SIZE = 400;

const gallery = document.getElementById('gallery');
gallery.dataset.columns = '';
const fullsizeImage = document.getElementById('fullsize-image');
const fullsizeContainer = document.getElementById('fullsize-container');
const fullsizePrev = document.getElementById('fullsize-prev');
const fullsizeNext = document.getElementById('fullsize-next');

let currentPage = 0;
let totalPages = 0;

let currentlyActiveFullscreenImageId = null;

// Fetch the total number of pages
axios.get('/media/page/count')
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

function loadNextPage() {
    if (currentPage < totalPages) {
        axios.get(`/media/page/${currentPage}`)
            .then(response => {
                let lastPlaceholder;
                response.data.ids.forEach(id => {
                    const placeholder = document.createElement('div');
                    placeholder.className = 'gallery-item-container';
                    placeholder.dataset.id = id;
                    gallery.appendChild(placeholder);
                    imageObserver.observe(placeholder);
                    lastPlaceholder = placeholder;
                    placeholder.onclick = () => {
                        showFullSizeImage(placeholder);
                    };
                });
                currentPage++;

                // Observe the last image placeholder to trigger the pageObserver
                if (lastPlaceholder) {
                    pageObserver.observe(lastPlaceholder);
                }
            });
    }
}

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
    root: gallery,
    rootMargin: '200px'  // Load/unload images/videos when they're within 200px of the visible area
});

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

function showFullSizeImage(placeholder) {
    if (!fullsizeContainer.classList.contains('open')) {
        while (fullsizeImage.firstChild) {
            fullsizeImage.removeChild(fullsizeImage.firstChild);
        }
    }

    const id = placeholder.dataset.id;
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

function hideFullSizeImage() {
    fullsizeContainer.classList.remove('open');
    fullsizeImage.classList.remove('loaded');
    while (fullsizeImage.firstChild) {
        fullsizeImage.removeChild(fullsizeImage.firstChild);
    }
    currentlyActiveFullscreenImageId = null;
}

function nextFullSizeImage() {
    const nextImage = document.querySelector(`.gallery-item-container[data-id="${currentlyActiveFullscreenImageId}"]`).nextSibling;
    if (nextImage) {
        showFullSizeImage(nextImage);
    } else {
        loadNextPage();
    }
}

function previousFullSizeImage() {
    const previousImage = document.querySelector(`.gallery-item-container[data-id="${currentlyActiveFullscreenImageId}"]`).previousSibling;
    if (previousImage) {
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

function openSettingsModal() {
    const myModal = new bootstrap.Modal(document.getElementById('settingsModal'), {});
    myModal.show();
    populateSettingsModalData();
}

function hideSettingsModal() {
    const myModal = bootstrap.Modal.getInstance(document.getElementById('settingsModal'));
    myModal.hide();
}

function populateSettingsModalData() {
    axios.get('/settings/get')
        .then(response => {
            const settings = response.settings;
        });
}