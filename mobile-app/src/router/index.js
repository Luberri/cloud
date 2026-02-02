import { createRouter, createWebHistory } from 'vue-router'
import Map from '../components/Map.vue'
import SignalForm from '../components/SignalForm.vue'
import Test from '../components/Test.vue'

const routes = [
  {
    path: '/',
    name: 'Map',
    component: Map
  },
  {
    path: '/signal/new/:typeId',
    name: 'NewSignal',
    component: SignalForm,
    props: true
  },
  { path: '/test', name: 'Test', component: Test }

]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router