
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
    document.getElementById(`filter-${filter}`).classList.add('active');
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
            div.className = 'white-panel p-5 flex justify-between items-center';
            div.innerHTML = `
                <div>
                    <span class="status-badge ${isPub ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'} mb-2 inline-block">${d.status}</span>
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

// ==========================
// STATISTICS
// ==========================
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
            let userName = "Unknown User";
            
            if (data.userId) {
                try {
                    const userDoc = await db.collection("users").doc(data.userId).get();
                    if (userDoc.exists) userName = userDoc.data().name || "Citizen";
                } catch (e) { console.log(e); }
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
    
    // 1. Badge Logic
    let badgeClass = 'bg-blue-100 text-blue-700';
    if (data.status === 'In Progress') badgeClass = 'bg-emerald-100 text-emerald-700';
    else if (data.status === 'On Hold') badgeClass = 'bg-amber-100 text-amber-700';
    else if (data.status === 'Resolved') badgeClass = 'bg-slate-200 text-slate-600';

    // 2. Define Tailwind Button Styles (Directly in JS to ensure colors work)
    const baseBtnStyle = "px-4 py-2 text-xs font-bold text-white uppercase rounded-lg shadow-md transition-all transform hover:-translate-y-0.5 border-none cursor-pointer";
    const btnGreen = `${baseBtnStyle} bg-emerald-500 hover:bg-emerald-600`;
    const btnYellow = `${baseBtnStyle} bg-amber-500 hover:bg-amber-600`;
    const btnRed = `${baseBtnStyle} bg-red-500 hover:bg-red-600`;
    const btnDisabled = "px-4 py-2 text-xs font-bold text-slate-400 uppercase bg-slate-200 rounded-lg cursor-not-allowed border-none";

    // 3. Button Workflow Logic
    let actionButtonsHtml = '';

    // CASE 1: Submitted -> Show "In Progress" (Green)
    if (!data.status || data.status === 'Submitted') {
        actionButtonsHtml = `
            <button onclick="updateStatus('${docId}', 'In Progress')" class="${btnGreen}">
                In Progress
            </button>
        `;
    } 
    // CASE 2: In Progress -> Show "Mark Resolved" (Green) + "On Hold" (Yellow)
    else if (data.status === 'In Progress') {
        actionButtonsHtml = `
            <button onclick="updateStatus('${docId}', 'Resolved')" class="${btnGreen}">
                Mark Resolved
            </button>
            <button onclick="updateStatus('${docId}', 'On Hold')" class="${btnYellow}">
                On Hold
            </button>
        `;
    } 
    // CASE 3: On Hold -> Show "Resume Progress" (Green)
    else if (data.status === 'On Hold') {
        actionButtonsHtml = `
            <button onclick="updateStatus('${docId}', 'In Progress')" class="${btnGreen}">
                Resume Progress
            </button>
        `;
    } 
    // CASE 4: Resolved -> Show "Delete" (Red) only
    else if (data.status === 'Resolved') {
        actionButtonsHtml = `
            <button onclick="deleteGrievance('${docId}')" class="${btnRed}">
                Delete Record
            </button>
        `;
    }

    // 4. Render Card
    const card = document.createElement('div');
    // Using Tailwind classes for the card container
    card.className = 'bg-white border border-slate-200 rounded-xl shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between overflow-hidden mb-6'; 
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

            <p class="text-slate-600 text-sm leading-relaxed mb-6">${data.description}</p>
            
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

// ... rest of your script ...
// ==========================
// ACTIONS
// ==========================
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

// ==========================
// INITIALIZATION
// ==========================
if(sessionStorage.getItem('loggedInUserEmail')) { 
    document.getElementById('welcome-message').innerText = sessionStorage.getItem('loggedInUserEmail');
    showView('department-selection-view');
    loadStats(); 
} else { 
    showView('login-view'); 
}