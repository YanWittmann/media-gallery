const THUMBNAIL_SIZE = 400;

const gallery = document.getElementById('gallery');
gallery.dataset.columns = '';

let currentPage = 0;
let totalPages = 0;

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
                    placeholder.className = 'placeholder';
                    placeholder.dataset.id = id;
                    gallery.appendChild(placeholder);
                    imageObserver.observe(placeholder);
                    lastPlaceholder = placeholder;
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
    const id = placeholder.dataset.id;
    const img = document.createElement('img');
    img.src = `/media/get/${id}/thumb/${THUMBNAIL_SIZE}`;
    img.onload = () => {
        placeholder.appendChild(img);
    };
    img.onerror = (error) => {
        console.error(`Error loading image with ID ${id}:`, error);
    };
}


function unloadImage(placeholder) {
    while (placeholder.firstChild) {
        placeholder.removeChild(placeholder.firstChild);
    }
}
