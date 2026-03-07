const firebaseConfig = { 
    apiKey: "AIzaSyAy7ibJyu2Qn_BPTOT-Kmu-aGQ5X9jjOZ0", 
    authDomain: "urban-management-platform.firebaseapp.com", 
    projectId: "urban-management-platform", 
    storageBucket: "urban-management-platform.appspot.com", 
    messagingSenderId: "546491864177", 
    appId: "1:546491864177:web:8c330c7f7b944a5fd337d0" 
};
firebase.initializeApp(firebaseConfig);
const db = firebase.firestore();

const OLA_MAPS_API_KEY = "ENTER_YOUR_API_KEY_HERE"; 

window.openMapModal = async (lng, lat, addressText) => {
    document.getElementById('map-modal').classList.remove('hidden');
    const mapContainer = document.getElementById('map');
    mapContainer.innerHTML = ''; 
    
    try {
        const olaMaps = new OlaMaps({ apiKey: OLA_MAPS_API_KEY });
        
        const myMap = await olaMaps.init({
            style: "https://api.olamaps.io/tiles/vector/v1/styles/default-light-standard/style.json",
            container: 'map',
            center: [lng, lat], 
            zoom: 16, 
        });

        const popup = new OlaMaps.Popup({ offset: [0, -30] })
            .setHTML(`<div style="padding: 8px; font-family: Inter, sans-serif; font-size: 12px; font-weight: 600; color: #1e293b; max-width: 200px;">${addressText}</div>`);

        new OlaMaps.Marker({ color: '#ef4444' }) 
            .setLngLat([lng, lat])
            .setPopup(popup)
            .addTo(myMap);

    } catch (error) {
        console.error("Error loading Ola Maps:", error);
        mapContainer.innerHTML = `<div class="w-full h-full flex items-center justify-center text-red-500 font-bold">Failed to load map. Check API Key.</div>`;
    }
};

window.closeMapModal = () => {
    document.getElementById('map-modal').classList.add('hidden');
};

let unsubscribeFromGrievances = null;
let currentFilter = 'all';

function showView(viewId) {
    const views = ['login-view', 'dashboard-view', 'department-selection-view', 'grievance-list-view', 'announcement-view'];
    views.forEach(id => {
        document.getElementById(id).classList.add('hidden');
    });
    
    document.getElementById(viewId).classList.remove('hidden');
    
    if(viewId !== 'login-view') {
        document.getElementById('dashboard-view').classList.remove('hidden');
    }
}

document.getElementById('login-form').onsubmit = (e) => {
    e.preventDefault();
    const email = document.getElementById('UserID').value;
    const pass = document.getElementById('login-password').value;
    const errorEl = document.getElementById('login-error');

    db.collection("officials").where("UserID", "==", email).get().then(snap => {
        if(!snap.empty && snap.docs[0].data().password === pass) {
            sessionStorage.setItem('loggedInUserEmail', email);
            window.location.reload();
        } else {
            errorEl.classList.remove('hidden');
        }
    });
};

document.getElementById('logout-button').onclick = () => {
    sessionStorage.clear();
    window.location.reload();
};

function showAnnouncementView() {
    showView('announcement-view');
    loadAnnouncements();
}

function filterAnnouncements(filter) {
    currentFilter = filter;
    document.querySelectorAll('.filter-btn').forEach(btn => btn.classList.remove('active')); 
    document.getElementById(`filter-${filter}`).classList.add('bg-blue-100', 'text-blue-700'); 
    loadAnnouncements();
}

document.getElementById('announcement-form').onsubmit = async (e) => {
    e.preventDefault();
    try {
        await db.collection("announcements").add({
            title: document.getElementById('ann-title').value,
            content: document.getElementById('ann-content').value,
            status: 'published',
            author: sessionStorage.getItem('loggedInUserEmail'),
            timestamp: firebase.firestore.FieldValue.serverTimestamp()
        });
        document.getElementById('announcement-form').reset();
    } catch (err) { alert(err.message); }
};

function loadAnnouncements() {
    let query = db.collection("announcements").orderBy("timestamp", "desc");
    if (currentFilter !== 'all') query = query.where("status", "==", currentFilter);

    query.onSnapshot(snap => {
        const container = document.getElementById('announcement-history-container');
        container.innerHTML = '';
        snap.forEach(doc => {
            const d = doc.data();
            const isPub = d.status === 'published';
            const div = document.createElement('div');
            div.className = 'bg-white p-5 flex justify-between items-center rounded-xl border border-slate-200 shadow-sm';
            div.innerHTML = `
                <div>
                    <span class="px-2 py-1 text-[10px] font-bold uppercase rounded-md ${isPub ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'} mb-2 inline-block">${d.status}</span>
                    <h4 class="font-bold text-slate-800">${d.title}</h4>
                    <p class="text-slate-500 text-xs mt-1">${d.content}</p>
                </div>
                <div class="flex flex-col gap-2 items-end">
                    <button onclick="updateAnnStatus('${doc.id}', '${d.status}')" class="text-[10px] font-bold text-blue-600 uppercase hover:underline">${isPub ? 'Withdraw' : 'Restore'}</button>
                    <button onclick="deleteAnn('${doc.id}')" class="text-[10px] font-bold text-red-500 uppercase hover:underline">Delete</button>
                </div>`;
            container.appendChild(div);
        });
    });
}

window.updateAnnStatus = (id, cur) => db.collection("announcements").doc(id).update({ status: cur === 'published' ? 'unpublished' : 'published' });
window.deleteAnn = (id) => confirm("Permanently archive this announcement?") && db.collection("announcements").doc(id).delete();

function loadStats() {
    db.collection("grievances").onSnapshot(snap => {
        let s=0, p=0, r=0;
        snap.forEach(doc => {
            const st = doc.data().status;
            if(st==='Submitted' || !st) s++; else if(st==='In Progress') p++; else if(st==='Resolved') r++;
        });
        document.getElementById('total-count').innerText = snap.size;
        document.getElementById('submitted-count').innerText = s;
        document.getElementById('progress-count').innerText = p;
        document.getElementById('resolved-count').innerText = r;
    });
}

window.showDepartmentGrievances = (dept) => {
    showView('grievance-list-view');
    document.getElementById('grievance-list-title').innerText = `${dept} Records`;
    const spinner = document.getElementById('loading-spinner');
    const container = document.getElementById('grievance-list-container');
    
    spinner.classList.remove('hidden');
    container.innerHTML = '';

    if (unsubscribeFromGrievances) unsubscribeFromGrievances();

    const q = db.collection("grievances")
        .where("department", "==", dept)
        .orderBy("timestamp", "desc");

    unsubscribeFromGrievances = q.onSnapshot(async (snapshot) => {
        spinner.classList.add('hidden');
        container.innerHTML = '';

        if (snapshot.empty) {
            container.innerHTML = '<p class="text-slate-500 col-span-2 text-center py-10">No records found for this department.</p>';
            return;
        }

        const promises = snapshot.docs.map(async (doc) => {
            const data = doc.data();
            let userName = data.userName || "Unknown User"; 
            
            if ((!data.userName || data.userName === "Anonymous") && data.userId) {
                try {
                    const userDoc = await db.collection("users").doc(data.userId).get();
                    if (userDoc.exists && userDoc.data().name) {
                        userName = userDoc.data().name;
                    }
                } catch (e) { console.log("Error fetching user:", e); }
            }
            return { id: doc.id, data, userName };
        });

        const results = await Promise.all(promises);

        results.forEach(item => {
            renderGrievanceCard(item.id, item.data, item.userName);
        });
    });
};

function renderGrievanceCard(docId, data, userName) {
    const container = document.getElementById('grievance-list-container');
    const date = data.timestamp ? data.timestamp.toDate().toLocaleString() : 'N/A';
    const address = data.address || "No address provided"; 
    
    let badgeClass = 'bg-blue-100 text-blue-700';
    if (data.status === 'In Progress') badgeClass = 'bg-emerald-100 text-emerald-700';
    else if (data.status === 'On Hold') badgeClass = 'bg-amber-100 text-amber-700';
    else if (data.status === 'Resolved') badgeClass = 'bg-slate-200 text-slate-600';

    const baseBtnStyle = "px-4 py-2 text-xs font-bold text-white uppercase rounded-lg shadow-md transition-all transform hover:-translate-y-0.5 border-none cursor-pointer";
    const btnGreen = `${baseBtnStyle} bg-emerald-500 hover:bg-emerald-600`;
    const btnYellow = `${baseBtnStyle} bg-amber-500 hover:bg-amber-600`;
    const btnRed = `${baseBtnStyle} bg-red-500 hover:bg-red-600`;

    let actionButtonsHtml = '';

    if (!data.status || data.status === 'Submitted') {
        actionButtonsHtml = `<button onclick="updateStatus('${docId}', 'In Progress')" class="${btnGreen}">In Progress</button>`;
    } 
    else if (data.status === 'In Progress') {
        actionButtonsHtml = `
            <button onclick="updateStatus('${docId}', 'Resolved')" class="${btnGreen}">Mark Resolved</button>
            <button onclick="updateStatus('${docId}', 'On Hold')" class="${btnYellow}">On Hold</button>
        `;
    } 
    else if (data.status === 'On Hold') {
        actionButtonsHtml = `<button onclick="updateStatus('${docId}', 'In Progress')" class="${btnGreen}">Resume Progress</button>`;
    } 
    else if (data.status === 'Resolved') {
        actionButtonsHtml = `<button onclick="deleteGrievance('${docId}')" class="${btnRed}">Delete Record</button>`;
    }

    let addressHtml = `<span class="text-xs font-medium text-slate-600 break-words">${address}</span>`;
    
    
    const safeAddress = address.replace(/'/g, "\\'").replace(/"/g, '&quot;');

    if (data.location && data.location.latitude && data.location.longitude) {
        addressHtml = `
            <span onclick="openMapModal(${data.location.longitude}, ${data.location.latitude}, '${safeAddress}')" 
                  class="text-xs font-medium text-blue-600 break-words cursor-pointer hover:underline flex items-center gap-1 group">
                ${address} 
                <span class="bg-blue-50 text-blue-600 group-hover:bg-blue-100 px-1.5 py-0.5 rounded text-[9px] uppercase font-bold border border-blue-200 transition-colors ml-1 whitespace-nowrap">View Map</span>
            </span>
        `;
    }

    const card = document.createElement('div');
    card.className = 'bg-white border border-slate-200 rounded-xl shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between overflow-hidden mb-6 grievance-card'; 
    card.id = `card-${docId}`;
    
    card.innerHTML = `
        <div class="p-6">
            <div class="flex justify-between items-start mb-4">
                <div>
                    <span class="px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wide ${badgeClass} mb-2 inline-block">${data.status || 'Submitted'}</span>
                    <h3 class="font-bold text-lg text-slate-800">${data.title || 'Untitled'}</h3>
                </div>
                <span class="text-xs text-slate-400 font-medium text-right">${date}</span>
            </div>

            <p class="text-slate-600 text-sm leading-relaxed mb-4">${data.description}</p>
            
            <div class="flex items-start gap-2 mb-4 p-2 bg-slate-50 rounded-lg">
                <svg class="w-4 h-4 text-slate-400 mt-0.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                ${addressHtml}
            </div>

            <div class="flex items-center gap-3 p-3 bg-slate-50 rounded-lg border border-slate-100">
                <div class="w-8 h-8 rounded-full bg-white border border-slate-200 flex items-center justify-center text-slate-600 font-bold text-xs shadow-sm">
                    ${userName.charAt(0).toUpperCase()}
                </div>
                <div>
                    <p class="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Reported By</p>
                    <p class="text-xs font-semibold text-slate-700">${userName}</p>
                </div>
            </div>

            ${data.imageUrl ? `
                <div class="mt-4">
                    <img src="${data.imageUrl}" alt="Evidence" class="w-full h-48 object-cover rounded-lg border border-slate-200 cursor-pointer hover:opacity-90 transition-opacity" onclick="window.open('${data.imageUrl}', '_blank')">
                </div>
            ` : ''}
        </div>

        <div class="bg-slate-50 px-6 py-4 border-t border-slate-100 flex justify-end gap-3">
            ${actionButtonsHtml}
        </div>
    `;

    container.appendChild(card);
}

window.updateStatus = (docId, newStatus) => {
    db.collection("grievances").doc(docId).update({ status: newStatus })
      .catch(err => alert("Error updating status"));
};

window.deleteGrievance = (docId) => {
    if (confirm("Are you sure you want to delete this record?")) {
        db.collection("grievances").doc(docId).delete()
          .catch(err => alert("Error deleting record"));
    }
};

window.searchGrievances = () => {
    const val = document.getElementById('grievance-search').value.toLowerCase();
    document.querySelectorAll('.grievance-card').forEach(card => {
        card.style.display = card.innerText.toLowerCase().includes(val) ? 'flex' : 'none';
    });
};

if(sessionStorage.getItem('loggedInUserEmail')) { 
    document.getElementById('welcome-message').innerText = sessionStorage.getItem('loggedInUserEmail');
    showView('department-selection-view');
    loadStats(); 
} else { 
    showView('login-view'); 
}
